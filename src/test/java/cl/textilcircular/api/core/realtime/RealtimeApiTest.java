package cl.textilcircular.api.core.realtime;

import cl.textilcircular.api.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** The websocket needs a real port: MockMvc cannot upgrade a connection. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Sql(statements = "TRUNCATE user_account CASCADE", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RealtimeApiTest {

    @LocalServerPort
    int port;
    @Autowired
    RealtimeSessions realtime;
    @Autowired
    JsonMapper json;

    final HttpClient http = HttpClient.newHttpClient();

    record Session(String access, String refresh, long userId) {
    }

    Session signUp() throws Exception {
        JsonNode body = json.readTree(post("/api/v1/auth/register/customer",
                "{\"email\":\"ws@demo.cl\",\"password\":\"Secreta123\",\"alias\":\"Ws\"}").body());
        return new Session(body.get("accessToken").asString(), body.get("refreshToken").asString(),
                body.get("user").get("id").asLong());
    }

    HttpResponse<String> post(String path, String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Collects the first text frame the server pushes. */
    static class Listener implements WebSocket.Listener {
        final CompletableFuture<String> firstFrame = new CompletableFuture<>();
        final CompletableFuture<Integer> closeCode = new CompletableFuture<>();
        final StringBuilder partial = new StringBuilder();

        @Override
        public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
            closeCode.complete(statusCode);
            return null;
        }

        @Override
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                firstFrame.complete(partial.toString());
            }
            socket.request(1);
            return null;
        }
    }

    WebSocket open(String path, String token, Listener listener) throws Exception {
        WebSocket.Builder builder = http.newWebSocketBuilder();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        try {
            return builder.buildAsync(URI.create("ws://localhost:" + port + path), listener).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw e.getCause() instanceof Exception cause ? cause : e;
        }
    }

    static int refusedStatus(ThrowingRunnable open) {
        try {
            open.run();
        } catch (WebSocketHandshakeException e) {
            return e.getResponse().statusCode();
        } catch (Exception e) {
            throw new AssertionError("Expected a refused handshake", e);
        }
        throw new AssertionError("The handshake was accepted");
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }

    static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Condition not met within 5 s");
            }
            Thread.sleep(50);
        }
    }

    @Test
    void a_valid_token_opens_the_socket_at_the_prefixed_path() throws Exception {
        Session session = signUp();

        WebSocket socket = open("/api/v1/ws", session.access(), new Listener());

        assertThat(socket.isInputClosed()).isFalse();
        socket.abort();
    }

    @Test
    void the_handshake_without_a_token_is_401() {
        assertThat(refusedStatus(() -> open("/api/v1/ws", null, new Listener()))).isEqualTo(401);
    }

    @Test
    void a_token_of_a_closed_session_is_401() throws Exception {
        Session session = signUp();
        assertThat(post("/api/v1/auth/logout", "{\"refreshToken\":\"%s\"}".formatted(session.refresh())).statusCode())
                .isEqualTo(204);

        assertThat(refusedStatus(() -> open("/api/v1/ws", session.access(), new Listener()))).isEqualTo(401);
    }

    @Test
    void the_unprefixed_path_is_not_a_socket() throws Exception {
        Session session = signUp();

        assertThat(refusedStatus(() -> open("/ws", session.access(), new Listener()))).isEqualTo(404);
    }

    @Test
    void after_logout_the_open_socket_gets_no_event_and_is_closed() throws Exception {
        Session session = signUp();
        Listener listener = new Listener();
        open("/api/v1/ws", session.access(), listener);
        awaitTrue(() -> realtime.connectionCount(session.userId()) == 1);
        post("/api/v1/auth/logout", "{\"refreshToken\":\"%s\"}".formatted(session.refresh()));

        realtime.send(session.userId(), "message.read", Map.of("conversationId", 31));

        assertThat(listener.closeCode.get(5, TimeUnit.SECONDS)).isEqualTo(1008);
        assertThat(listener.firstFrame).isNotDone();
        awaitTrue(() -> realtime.connectionCount(session.userId()) == 0);
    }

    @Test
    void pushed_events_arrive_as_typed_frames_with_their_nulls() throws Exception {
        Session session = signUp();
        Listener listener = new Listener();
        WebSocket socket = open("/api/v1/ws", session.access(), listener);
        awaitTrue(() -> realtime.connectionCount(session.userId()) == 1);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", 812);
        data.put("conversationId", 31);
        data.put("readAt", null);
        realtime.send(session.userId(), "message.created", data);

        assertThat(listener.firstFrame.get(5, TimeUnit.SECONDS))
                .isEqualTo("{\"type\":\"message.created\",\"data\":{\"id\":812,\"conversationId\":31,\"readAt\":null}}");
        socket.sendClose(WebSocket.NORMAL_CLOSURE, "").get(5, TimeUnit.SECONDS);
        awaitTrue(() -> realtime.connectionCount(session.userId()) == 0);
        assertThatCode(() -> realtime.send(session.userId(), "message.read", data)).doesNotThrowAnyException();
    }
}

package cl.textilcircular.api.core.realtime;

import cl.textilcircular.api.core.repository.RefreshTokenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Delivery rules that a real socket cannot reproduce on demand: failures, stalls and expiry. */
class RealtimeSessionsTest {

    final Clock clock = Clock.systemUTC();
    final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    final RealtimeSessions sessions = new RealtimeSessions(JsonMapper.builder().build(), refreshTokens, clock);
    final WebSocketSession socket = mock(WebSocketSession.class);

    RealtimeSessionsTest() {
        when(refreshTokens.isActive(anyLong(), any())).thenReturn(true);
        when(socket.getId()).thenReturn("s1");
        when(socket.isOpen()).thenReturn(true);
    }

    @AfterEach
    void shutDown() {
        sessions.destroy();
    }

    Instant inAnHour() {
        return clock.instant().plus(Duration.ofHours(1));
    }

    @Test
    void a_failed_send_closes_the_socket_so_the_client_reconnects() throws Exception {
        doThrow(new IOException("broken pipe")).when(socket).sendMessage(any());
        sessions.add(7, 1, inAnHour(), socket);

        sessions.send(7, "message.read", Map.of("conversationId", 31));

        verify(socket, timeout(2000)).close(RealtimeSessions.UNRELIABLE);
        assertThat(sessions.connectionCount(7)).isZero();
    }

    @Test
    void a_failed_session_check_closes_the_socket_instead_of_stalling_it() throws Exception {
        when(refreshTokens.isActive(anyLong(), any())).thenThrow(new IllegalStateException("pool exhausted"));
        sessions.add(7, 1, inAnHour(), socket);

        sessions.send(7, "message.read", Map.of("conversationId", 31));

        verify(socket, timeout(2000)).close(RealtimeSessions.UNRELIABLE);
        assertThat(sessions.connectionCount(7)).isZero();
    }

    @Test
    void send_returns_while_a_socket_is_stalled_and_keeps_the_order() throws Exception {
        CountDownLatch stalled = new CountDownLatch(1);
        doAnswer(call -> {
            stalled.await();
            return null;
        }).when(socket).sendMessage(any());
        sessions.add(7, 1, inAnHour(), socket);

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            sessions.send(7, "first", Map.of());
            sessions.send(7, "second", Map.of());
        });
        stalled.countDown();

        InOrder order = inOrder(socket);
        order.verify(socket, timeout(2000)).sendMessage(new TextMessage("{\"type\":\"first\",\"data\":{}}"));
        order.verify(socket, timeout(2000)).sendMessage(new TextMessage("{\"type\":\"second\",\"data\":{}}"));
    }

    @Test
    void the_socket_closes_when_its_access_token_expires() throws Exception {
        sessions.add(7, 1, clock.instant().plusMillis(200), socket);

        verify(socket, timeout(2000)).close(RealtimeSessions.SESSION_ENDED);
        assertThat(sessions.connectionCount(7)).isZero();
    }
}

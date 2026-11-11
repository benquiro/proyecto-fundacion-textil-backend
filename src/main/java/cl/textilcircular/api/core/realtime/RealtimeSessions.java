package cl.textilcircular.api.core.realtime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The open sockets of each user, and the one way features push an event to them: {@link #send}.
 * Safe to call from any thread.
 */
@Slf4j
@Component
public class RealtimeSessions {

    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    /** A frame of the contract's x-websocket channel. */
    record Event(String type, Object data) {
    }

    private final Map<Long, Map<String, WebSocketSession>> byUser = new ConcurrentHashMap<>();
    private final JsonMapper json;

    RealtimeSessions(JsonMapper json) {
        this.json = json;
    }

    void add(long userId, WebSocketSession session) {
        // A raw WebSocketSession must not be sent to from two threads at once; the decorator serializes sends.
        WebSocketSession concurrent = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MILLIS,
                BUFFER_SIZE_LIMIT_BYTES);
        // Inside compute: remove() may drop an emptied entry at the same moment, and a put on that
        // dropped map would lose this socket.
        byUser.compute(userId, (id, sessions) -> {
            Map<String, WebSocketSession> current = sessions == null ? new ConcurrentHashMap<>() : sessions;
            current.put(session.getId(), concurrent);
            return current;
        });
    }

    void remove(long userId, WebSocketSession session) {
        byUser.computeIfPresent(userId, (id, sessions) -> {
            sessions.remove(session.getId());
            return sessions.isEmpty() ? null : sessions;
        });
    }

    /** Sends {@code {"type":..., "data":...}} to every open socket of the user; does nothing when there is none. */
    public void send(long userId, String type, Object data) {
        Map<String, WebSocketSession> sessions = byUser.get(userId);
        if (sessions == null) {
            return;
        }
        TextMessage message = new TextMessage(json.writeValueAsString(new Event(type, data)));
        sessions.values().forEach(session -> {
            try {
                session.sendMessage(message);
            } catch (IOException | RuntimeException e) {
                // One broken socket (closed, or over the decorator's limits) must not stop the others.
                log.debug("Dropping socket {} of user {}: {}", session.getId(), userId, e.toString());
                remove(userId, session);
            }
        });
    }

    public int connectionCount(long userId) {
        Map<String, WebSocketSession> sessions = byUser.get(userId);
        return sessions == null ? 0 : sessions.size();
    }
}

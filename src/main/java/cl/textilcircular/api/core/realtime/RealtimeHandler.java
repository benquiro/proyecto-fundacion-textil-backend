package cl.textilcircular.api.core.realtime;

import cl.textilcircular.api.core.security.Caller;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Tracks each authenticated socket under its user. The handshake already went through the bearer-token
 * filter chain, so the principal is the caller's verified access token, which always carries an expiry.
 */
@Component
@RequiredArgsConstructor
class RealtimeHandler extends TextWebSocketHandler {

    private static final String USER_ID = "userId";

    private final RealtimeSessions sessions;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!(session.getPrincipal() instanceof JwtAuthenticationToken auth)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        Caller caller = Caller.of(auth.getToken());
        session.getAttributes().put(USER_ID, caller.userId());
        sessions.add(caller.userId(), caller.sessionId(), auth.getToken().getExpiresAt(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (session.getAttributes().get(USER_ID) instanceof Long userId) {
            sessions.remove(userId, session);
        }
    }

    /** The contract's channel is server-to-client only: client frames are ignored. */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
    }
}

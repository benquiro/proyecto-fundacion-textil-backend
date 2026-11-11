package cl.textilcircular.api.core.realtime;

import cl.textilcircular.api.core.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The open sockets of each user, and the one way features push an event to them: {@link #send}.
 * Safe to call from any thread, and it never blocks: each socket delivers its events in order on a
 * virtual thread. A socket only gets events while the access token it opened with is valid and its
 * session is active; otherwise it is closed and the client reconnects with a fresh token.
 */
@Slf4j
@Component
public class RealtimeSessions implements DisposableBean {

    /** The access token expired or the session ended (logout, password change, suspension...). */
    static final CloseStatus SESSION_ENDED = CloseStatus.POLICY_VIOLATION.withReason("Session ended");
    /** A send failed, stalled or backed up: the client reconnects and catches up through REST. */
    static final CloseStatus UNRELIABLE = CloseStatus.SESSION_NOT_RELIABLE;

    private static final int MAX_QUEUED_EVENTS = 100;
    private static final long SEND_TIME_LIMIT_NANOS = TimeUnit.SECONDS.toNanos(10);

    /** A frame of the contract's x-websocket channel. */
    record Event(String type, Object data) {
    }

    private final Map<Long, Map<String, Socket>> byUser = new ConcurrentHashMap<>();
    private final ExecutorService senders = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService expiries = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("realtime-expiry").factory());
    private final JsonMapper json;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    RealtimeSessions(JsonMapper json, RefreshTokenRepository refreshTokens, Clock clock) {
        this.json = json;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    /** Registers a socket opened with the access token of {@code sessionId}, valid until {@code expiresAt}. */
    void add(long userId, long sessionId, Instant expiresAt, WebSocketSession session) {
        Socket socket = new Socket(userId, sessionId, expiresAt, session);
        // Inside compute: remove() may drop an emptied entry at the same moment, and a put on that
        // dropped map would lose this socket.
        byUser.compute(userId, (id, sockets) -> {
            Map<String, Socket> current = sockets == null ? new ConcurrentHashMap<>() : sockets;
            current.put(session.getId(), socket);
            return current;
        });
        long delay = Math.max(0, Duration.between(clock.instant(), expiresAt).toMillis());
        socket.expiry = expiries.schedule(() -> socket.close(SESSION_ENDED), delay, TimeUnit.MILLISECONDS);
    }

    void remove(long userId, WebSocketSession session) {
        byUser.computeIfPresent(userId, (id, sockets) -> {
            Socket removed = sockets.remove(session.getId());
            if (removed != null && removed.expiry != null) {
                removed.expiry.cancel(false);
            }
            return sockets.isEmpty() ? null : sockets;
        });
    }

    /**
     * Queues {@code {"type":..., "data":...}} for every open socket of the user and returns at once;
     * does nothing when there is none.
     */
    public void send(long userId, String type, Object data) {
        Map<String, Socket> sockets = byUser.get(userId);
        if (sockets == null) {
            return;
        }
        TextMessage message = new TextMessage(json.writeValueAsString(new Event(type, data)));
        sockets.values().forEach(socket -> socket.offer(message));
    }

    public int connectionCount(long userId) {
        Map<String, Socket> sockets = byUser.get(userId);
        return sockets == null ? 0 : sockets.size();
    }

    @Override
    public void destroy() {
        expiries.shutdownNow();
        senders.shutdownNow();
    }

    /** One open socket: its pending events, drained by at most one virtual thread at a time. */
    private final class Socket {

        private final long userId;
        private final long sessionId;
        private final Instant expiresAt;
        private final WebSocketSession session;
        private final Queue<TextMessage> pending = new ConcurrentLinkedQueue<>();
        private final AtomicInteger pendingCount = new AtomicInteger();
        private final AtomicBoolean draining = new AtomicBoolean();
        private final AtomicBoolean closed = new AtomicBoolean();
        /** {@link System#nanoTime()} when the current write started, 0 when idle. */
        private volatile long writeStartedAt;
        private volatile ScheduledFuture<?> expiry;

        Socket(long userId, long sessionId, Instant expiresAt, WebSocketSession session) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.expiresAt = expiresAt;
            this.session = session;
        }

        void offer(TextMessage message) {
            long started = writeStartedAt;
            if (pendingCount.get() >= MAX_QUEUED_EVENTS
                    || (started != 0 && System.nanoTime() - started > SEND_TIME_LIMIT_NANOS)) {
                close(UNRELIABLE);
                return;
            }
            pending.add(message);
            pendingCount.incrementAndGet();
            if (draining.compareAndSet(false, true)) {
                senders.execute(this::drain);
            }
        }

        private void drain() {
            while (!closed.get()) {
                TextMessage message = pending.poll();
                if (message == null) {
                    draining.set(false);
                    // An offer may have queued after the poll but seen draining still true.
                    if (pending.isEmpty() || !draining.compareAndSet(false, true)) {
                        return;
                    }
                    continue;
                }
                pendingCount.decrementAndGet();
                if (!authorized()) {
                    close(SESSION_ENDED);
                    return;
                }
                writeStartedAt = System.nanoTime();
                try {
                    session.sendMessage(message);
                } catch (IOException | RuntimeException e) {
                    log.debug("Closing socket {} of user {} after a failed send: {}", session.getId(), userId, e.toString());
                    close(UNRELIABLE);
                    return;
                } finally {
                    writeStartedAt = 0;
                }
            }
        }

        /** Checked on every delivery, so an ended session gets nothing even before its token expires. */
        private boolean authorized() {
            Instant now = clock.instant();
            return now.isBefore(expiresAt) && refreshTokens.isActive(sessionId, now);
        }

        /** Unregisters at once; the close itself runs off the caller's thread, since it can block too. */
        void close(CloseStatus status) {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            remove(userId, session);
            pending.clear();
            senders.execute(() -> {
                try {
                    session.close(status);
                } catch (IOException | RuntimeException e) {
                    log.debug("Socket {} of user {} did not close cleanly: {}", session.getId(), userId, e.toString());
                }
            });
        }
    }
}

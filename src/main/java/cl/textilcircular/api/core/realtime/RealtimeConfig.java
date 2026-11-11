package cl.textilcircular.api.core.realtime;

import cl.textilcircular.api.core.web.WebConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** The real-time channel of the contract (x-websocket): server-to-client events only. */
@Configuration(proxyBeanMethods = false)
@EnableWebSocket
class RealtimeConfig implements WebSocketConfigurer {

    /** Not a controller, so WebConfig's prefix does not reach it: the full path is spelled here. */
    static final String PATH = WebConfig.API_BASE_PATH + "/ws";

    private final RealtimeHandler handler;

    RealtimeConfig(RealtimeHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // No setAllowedOrigins: the app (OkHttp) sends no Origin header, which is accepted; foreign browsers are not.
        registry.addHandler(handler, PATH);
    }
}

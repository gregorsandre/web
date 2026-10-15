package tech.kood.backend.chat;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ChatSocketConfig implements WebSocketConfigurer {
    private final ChatSocketHandler handler;
    private final TokenHandshakeInterceptor tokenCheck;

    ChatSocketConfig(ChatSocketHandler handler, TokenHandshakeInterceptor tokenCheck) {
        this.handler = handler;
        this.tokenCheck = tokenCheck;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Any origin is fine: the socket is authorised by the token in the URL, not by cookies,
        // so another site can't open it on the user's behalf
        registry.addHandler(handler, "/ws")
                .addInterceptors(tokenCheck)
                .setAllowedOriginPatterns("*");
    }
}

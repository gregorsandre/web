package tech.kood.backend.chat;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import tech.kood.backend.auth.JwtService;

import java.util.Map;

// Browsers can't send an Authorization header when opening a WebSocket, so the JWT
// comes as ?token=... and is checked once, before the connection is accepted.
@Component
public class TokenHandshakeInterceptor implements HandshakeInterceptor {
    static final String USER_ID = "userId";

    private final JwtService jwt;

    TokenHandshakeInterceptor(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        if (token != null) {
            try {
                attributes.put(USER_ID, jwt.parse(token));
                return true;
            } catch (JwtException | IllegalArgumentException ignored) {
                // falls through to the refusal
            }
        }
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
    }
}

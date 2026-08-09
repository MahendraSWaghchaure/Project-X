package com.projectx.tracking.ws;

import com.projectx.common.security.CurrentUser;
import com.projectx.common.security.Role;
import java.security.Principal;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * The native WebSocket handshake can't carry an Authorization header, so instead we require the
 * access token on the STOMP CONNECT frame itself (a header the client sets regardless of
 * transport) and reject the connection if it's missing/invalid.
 */
@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    public WebSocketAuthChannelInterceptor(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new JwtException("Missing bearer token on STOMP CONNECT");
            }
            Jwt jwt = jwtDecoder.decode(authHeader.substring(7));
            accessor.setUser(toPrincipal(jwt));
        }
        return message;
    }

    private Principal toPrincipal(Jwt jwt) {
        Role role = Role.valueOf(jwt.getClaimAsString("role"));
        Long schoolId = jwt.hasClaim("schoolId") ? ((Number) jwt.getClaim("schoolId")).longValue() : null;
        CurrentUser currentUser = new CurrentUser(
                Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("email"), role, schoolId);
        return new StompPrincipal(currentUser);
    }

    /** Principal name must be stable/unique per session for Spring's user-destination routing (/user/queue/...). */
    public record StompPrincipal(CurrentUser currentUser) implements Principal {
        @Override
        public String getName() {
            return currentUser.userId().toString();
        }
    }
}

package org.example.backend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.example.backend.entity.Messages;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {
    private final SimpMessageSendingOperations messagingTemplate;
    private final UserRepository userRepository;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        if (userEmail != null) {
            log.info("User Connected : {}", userEmail);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userEmail = (String) headerAccessor.getSessionAttributes().get("userEmail");
        if (userEmail != null) {
            log.info("User Disconnected : {}", userEmail);
        }
    }

    @EventListener
    public void handleSessionConnectEvent(SessionConnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String userEmail = sha.getFirstNativeHeader("userEmail");
        if (userEmail != null) {
            sha.getSessionAttributes().put("userEmail", userEmail);
            log.info("User {} attempting to connect", userEmail);
        }
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String userEmail = (String) sha.getSessionAttributes().get("userEmail");
        if (userEmail != null) {
            log.info("User {} subscribed to {}", userEmail, sha.getDestination());
        }
    }
} 
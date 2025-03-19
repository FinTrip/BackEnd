package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend.dto.ApiResponse;
import org.example.backend.dto.MessageRequest;
import org.example.backend.entity.Messages;
import org.example.backend.entity.User;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    private final MessageService messageService;
    private final SimpMessageSendingOperations messagingTemplate;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @Valid @RequestBody MessageRequest messageRequest,
            HttpServletRequest request) {
        String userEmail = (String) request.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        try {
            Messages savedMessage = messageService.sendMessage(userEmail, messageRequest);
            User sender = savedMessage.getSender();
            User receiver = savedMessage.getReceiver();

            log.info("savedMessage: {}", savedMessage);
            log.info("sender: {}", sender);
            log.info("receiver: {}", receiver);

            Map<String, Object> responseData = new HashMap<>(
                    Map.of(
                            "id", Objects.requireNonNullElse(savedMessage.getId(), 0),
                            "content", Objects.requireNonNullElse(savedMessage.getContent(), ""),
                            "createdAt", Objects.requireNonNullElse(savedMessage.getCreatedAt(), LocalDateTime.now()),
                            "isRead", Objects.requireNonNullElse(savedMessage.getIsRead(), false),
                            "sender", new HashMap<>(
                                    Map.of(
                                            "id", Objects.requireNonNullElse(sender.getId(), 0),
                                            "email", Objects.requireNonNullElse(sender.getEmail(), ""),
                                            "fullName", Objects.requireNonNullElse(sender.getFullName(), "")
                                    )
                            ),
                            "receiver", new HashMap<>(
                                    Map.of(
                                            "id", Objects.requireNonNullElse(receiver.getId(), 0),
                                            "email", Objects.requireNonNullElse(receiver.getEmail(), ""),
                                            "fullName", Objects.requireNonNullElse(receiver.getFullName(), "")
                                    )
                            )
                    )
            );

            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error sending message", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllMessages(
            HttpServletRequest request) {
        String userEmail = (String) request.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        List<Messages> messages = messageService.getUserMessages(userEmail);
        List<Map<String, Object>> response = messages.stream()
                .map(message -> {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("id", message.getId());
                    messageMap.put("content", Objects.requireNonNullElse(message.getContent(), ""));
                    messageMap.put("createdAt", Objects.requireNonNullElse(message.getCreatedAt(), LocalDateTime.now()));
                    messageMap.put("isRead", Objects.requireNonNullElse(message.getIsRead(), false));
                    Map<String, Object> senderMap = new HashMap<>();
                    senderMap.put("id", message.getSender().getId());
                    senderMap.put("email", Objects.requireNonNullElse(message.getSender().getEmail(), ""));
                    senderMap.put("fullName", Objects.requireNonNullElse(message.getSender().getFullName(), ""));
                    messageMap.put("sender", senderMap);
                    Map<String, Object> receiverMap = new HashMap<>();
                    receiverMap.put("id", message.getReceiver().getId());
                    receiverMap.put("email", Objects.requireNonNullElse(message.getReceiver().getEmail(), ""));
                    receiverMap.put("fullName", Objects.requireNonNullElse(message.getReceiver().getFullName(), ""));
                    messageMap.put("receiver", receiverMap);
                    return messageMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/messages/{messageId}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            HttpServletRequest request,
            @PathVariable Integer messageId) {
        String userEmail = (String) request.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        messageService.markMessageAsRead(userEmail, messageId);
        return ResponseEntity.ok(ApiResponse.success("Message marked as read"));
    }
}
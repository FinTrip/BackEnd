package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.MessageRequest;
import org.example.backend.entity.Messages;
import org.example.backend.entity.User;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.repository.MessagesRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessagesRepository messagesRepository;
    private final UserRepository userRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    @Transactional
    public Messages sendMessage(String senderEmail, MessageRequest request) {
        try {
            User sender = userRepository.findByEmail(senderEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Sender not found"));

            User receiver = userRepository.findById(request.getReceiverId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Receiver not found"));

            if (sender.getId().equals(receiver.getId())) {
                throw new AppException(ErrorCode.INVALID_INPUT, "Cannot send message to yourself");
            }

            Messages message = new Messages();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setContent(request.getContent().trim());
            message.setIsRead(false);
            message.setCreatedAt(LocalDateTime.now());

            log.info("Sender: {}", sender.getId());
            log.info("Receiver: {}", receiver.getId());
            log.info("Saving message: {}", message);
            
            Messages savedMessage = messagesRepository.save(message);
            
            try {
                messagingTemplate.convertAndSendToUser(
                    receiver.getId().toString(),
                    "/queue/private",
                    savedMessage
                );
                log.info("Message sent to WebSocket");
            } catch (Exception e) {
                log.error("Error sending message to WebSocket: {}", e.getMessage());
            }
            
            return savedMessage;
        } catch (AppException e) {
            log.error("Application error sending message: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error sending message: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error sending message: " + e.getMessage());
        }
    }

    public List<Messages> getUserMessages(String userEmail) {
        try {
            log.info("Getting messages for user: {}", userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            log.info("User found: {}", user.getId());

            List<Messages> messages = messagesRepository.findByReceiverOrderByCreatedAtDesc(user);
            log.info("Found {} messages for user", messages.size());
            
            return messages;
        } catch (Exception e) {
            log.error("Error getting user messages: ", e);
            throw e;
        }
    }

    @Transactional
    public void markMessageAsRead(String userEmail, Integer messageId) {
        try {
            log.info("Marking message {} as read for user: {}", messageId, userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
            log.info("User found: {}", user.getId());

            Messages message = messagesRepository.findById(messageId)
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND, "Message not found"));
            log.info("Message found: {}", message.getId());

            if (!message.getReceiver().getId().equals(user.getId())) {
                log.warn("Unauthorized access attempt to message {} by user {}", messageId, userEmail);
                throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS, "Cannot mark other user's messages as read");
            }

            if (!message.getIsRead()) {
                message.setIsRead(true);
                messagesRepository.save(message);
                log.info("Message {} marked as read", messageId);
            } else {
                log.info("Message {} was already read", messageId);
            }
        } catch (AppException e) {
            log.error("Application error while marking message as read: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while marking message as read: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error marking message as read");
        }
    }

    public List<Messages> getUnreadMessages(String userEmail) {
        try {
            log.info("Getting unread messages for user: {}", userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            log.info("User found: {}", user.getId());

            List<Messages> messages = messagesRepository.findByReceiverAndIsReadFalseOrderByCreatedAtDesc(user);
            log.info("Found {} unread messages", messages.size());
            
            return messages;
        } catch (Exception e) {
            log.error("Error getting unread messages: ", e);
            throw e;
        }
    }
    
    public List<Messages> getLatestMessagesFromEachSender(String userEmail) {
        try {
            log.info("Getting latest messages from each sender for user: {}", userEmail);
            
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            log.info("User found: {}", user.getId());

            return messagesRepository.findLatestMessagesFromEachSender(user.getId());
        } catch (Exception e) {
            log.error("Error getting latest messages from each sender: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error retrieving chat list");
        }
    }

    public List<Messages> getConversationWithUser(String userEmail, Integer otherUserId) {
        try {
            log.info("Getting conversation between user {} and user {}", userEmail, otherUserId);
            
            User currentUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            
            User otherUser = userRepository.findById(otherUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            return messagesRepository.findBySenderAndReceiverOrReceiverAndSenderOrderByCreatedAtDesc(
                currentUser, otherUser, currentUser, otherUser);
        } catch (Exception e) {
            log.error("Error getting conversation: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error retrieving conversation");
        }
    }
} 
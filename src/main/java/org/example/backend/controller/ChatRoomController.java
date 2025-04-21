package org.example.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.AddMemberRequest;
import org.example.backend.dto.ApiResponse;
import org.example.backend.dto.ChatRoomRequest;
import org.example.backend.dto.GroupMessageRequest;
import org.example.backend.entity.ChatRoom;
import org.example.backend.entity.GroupMessage;
import org.example.backend.entity.User;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.service.ChatRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chatrooms")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createChatRoom(
            @Valid @RequestBody ChatRoomRequest request,
            HttpServletRequest httpRequest) {
        String userEmail = (String) httpRequest.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        try {
            ChatRoom chatRoom = chatRoomService.createChatRoom(userEmail, request);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", chatRoom.getId());
            responseData.put("name", chatRoom.getName());
            responseData.put("createdAt", chatRoom.getCreatedAt());
            
            Map<String, Object> creatorData = new HashMap<>();
            creatorData.put("id", chatRoom.getCreatedBy().getId());
            creatorData.put("email", chatRoom.getCreatedBy().getEmail());
            creatorData.put("fullName", chatRoom.getCreatedBy().getFullName());
            responseData.put("createdBy", creatorData);
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating chat room", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/members/add")
    public ResponseEntity<ApiResponse<String>> addMembers(
            @Valid @RequestBody AddMemberRequest request,
            HttpServletRequest httpRequest) {
        String userEmail = (String) httpRequest.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        chatRoomService.addMembersToRoom(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success("Members added successfully"));
    }

    @GetMapping("/my-rooms")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserChatRooms(
            HttpServletRequest httpRequest) {
        String userEmail = (String) httpRequest.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        List<ChatRoom> chatRooms = chatRoomService.getUserChatRooms(userEmail);
        List<Map<String, Object>> responseData = chatRooms.stream()
                .map(room -> {
                    Map<String, Object> roomData = new HashMap<>();
                    roomData.put("id", room.getId());
                    roomData.put("name", room.getName());
                    roomData.put("createdAt", room.getCreatedAt());
                    
                    Map<String, Object> creatorData = new HashMap<>();
                    creatorData.put("id", room.getCreatedBy().getId());
                    creatorData.put("email", room.getCreatedBy().getEmail());
                    creatorData.put("fullName", room.getCreatedBy().getFullName());
                    roomData.put("createdBy", creatorData);
                    
                    return roomData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @GetMapping("/{roomId}/members")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoomMembers(
            @PathVariable Integer roomId,
            HttpServletRequest httpRequest) {
        String userEmail = (String) httpRequest.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        List<User> members = chatRoomService.getChatRoomMembers(userEmail, roomId);
        List<Map<String, Object>> responseData = members.stream()
                .map(member -> {
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("id", member.getId());
                    memberData.put("email", member.getEmail());
                    memberData.put("fullName", member.getFullName());
                    return memberData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @PostMapping("/send-message")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendGroupMessage(
            @Valid @RequestBody GroupMessageRequest request,
            HttpServletRequest httpRequest) {
        String userEmail = (String) httpRequest.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }
        
        try {
            log.info("Sending group message - User: {}, Room: {}, Content: {}", 
                    userEmail, request.getRoomId(), request.getContent());
            
            GroupMessage message = chatRoomService.sendGroupMessage(userEmail, request);
            
            log.info("Message sent successfully - ID: {}", message.getId());
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", message.getId());
            responseData.put("content", message.getContent());
            responseData.put("createdAt", message.getCreatedAt());
            
            Map<String, Object> senderData = new HashMap<>();
            senderData.put("id", message.getSender().getId());
            senderData.put("email", message.getSender().getEmail());
            senderData.put("fullName", message.getSender().getFullName());
            responseData.put("sender", senderData);
            
            Map<String, Object> roomData = new HashMap<>();
            roomData.put("id", message.getChatRoom().getId());
            roomData.put("name", message.getChatRoom().getName());
            responseData.put("chatRoom", roomData);

            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (AppException e) {
            log.error("Application error while sending group message: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while sending group message: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error sending group message: " + e.getMessage());
        }
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRoomMessages(
            @PathVariable Integer roomId,
            HttpServletRequest httpRequest) {
        String userEmail = (String) httpRequest.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        List<GroupMessage> messages = chatRoomService.getRoomMessages(userEmail, roomId);
        List<Map<String, Object>> responseData = messages.stream()
                .map(message -> {
                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("id", message.getId());
                    messageData.put("content", message.getContent());
                    messageData.put("createdAt", message.getCreatedAt());
                    
                    Map<String, Object> senderData = new HashMap<>();
                    senderData.put("id", message.getSender().getId());
                    senderData.put("email", message.getSender().getEmail());
                    senderData.put("fullName", message.getSender().getFullName());
                    messageData.put("sender", senderData);
                    
                    return messageData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responseData));
    }

    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<String>> leaveRoom(
            @PathVariable Integer roomId,
            HttpServletRequest httpRequest) {
        String userEmail = (String) httpRequest.getAttribute("userEmail");
        if (userEmail == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED_USER);
        }

        chatRoomService.leaveRoom(userEmail, roomId);
        return ResponseEntity.ok(ApiResponse.success("Left room successfully"));
    }
} 
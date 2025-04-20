package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.AddMemberRequest;
import org.example.backend.dto.ChatRoomRequest;
import org.example.backend.dto.GroupMessageRequest;
import org.example.backend.entity.ChatRoom;
import org.example.backend.entity.GroupMessage;
import org.example.backend.entity.RoomMembers;
import org.example.backend.entity.User;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.repository.ChatRoomRepository;
import org.example.backend.repository.GroupMessageRepository;
import org.example.backend.repository.RoomMembersRepository;
import org.example.backend.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {
    
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMembersRepository roomMembersRepository;
    private final UserRepository userRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final FriendshipService friendshipService;

    @Transactional
    public ChatRoom createChatRoom(String creatorEmail, ChatRoomRequest request) {
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));

        // Tạo phòng chat mới
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(request.getName().trim());
        chatRoom.setCreatedBy(creator);
        chatRoom.setCreatedAt(LocalDateTime.now());
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        // Thêm người tạo vào phòng chat
        RoomMembers creatorMember = new RoomMembers();
        creatorMember.setChatRoom(savedRoom);
        creatorMember.setUser(creator);
        roomMembersRepository.save(creatorMember);
        
        // Thêm các thành viên khác (chỉ thêm những người là bạn bè)
        List<Integer> memberIds = request.getMemberIds();
        for (Integer memberId : memberIds) {
            if (memberId.equals(creator.getId())) {
                continue; // Bỏ qua nếu là người tạo (đã thêm ở trên)
            }
            
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Member not found with ID: " + memberId));
            
            // Kiểm tra xem người dùng có phải là bạn bè không
            if (!friendshipService.areFriends(creatorEmail, memberId)) {
                log.warn("Cannot add non-friend user {} to chat room", memberId);
                continue; // Bỏ qua nếu không phải bạn bè
            }
            
            RoomMembers roomMember = new RoomMembers();
            roomMember.setChatRoom(savedRoom);
            roomMember.setUser(member);
            roomMembersRepository.save(roomMember);
        }
        
        return savedRoom;
    }
    
    @Transactional
    public void addMembersToRoom(String userEmail, AddMemberRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        
        ChatRoom chatRoom = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ROOM_NOT_FOUND, "Chat room not found"));
        
        // Kiểm tra người dùng có quyền thêm thành viên không (phải là thành viên của phòng)
        Optional<RoomMembers> memberCheck = roomMembersRepository.findByUserAndChatRoom(user, chatRoom);
        if (memberCheck.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS, "You are not a member of this chat room");
        }
        
        // Thêm các thành viên mới (chỉ thêm những người là bạn bè)
        List<Integer> memberIds = request.getMemberIds();
        for (Integer memberId : memberIds) {
            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Member not found with ID: " + memberId));
            
            // Kiểm tra nếu đã là thành viên
            Optional<RoomMembers> existingMember = roomMembersRepository.findByUserAndChatRoom(member, chatRoom);
            if (existingMember.isPresent()) {
                log.warn("User {} is already a member of chat room {}", memberId, chatRoom.getId());
                continue; // Bỏ qua nếu đã là thành viên
            }   
            
            // Kiểm tra xem người dùng có phải là bạn bè không
            if (!friendshipService.areFriends(userEmail, memberId)) {
                log.warn("Cannot add non-friend user {} to chat room", memberId);
                throw new AppException(ErrorCode.NOT_FRIENDS, "You can only add friends to the chat room");
            }
            
            RoomMembers roomMember = new RoomMembers();
            roomMember.setChatRoom(chatRoom);
            roomMember.setUser(member);
            roomMembersRepository.save(roomMember);
        }
    }
    
    public List<ChatRoom> getUserChatRooms(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        
        return roomMembersRepository.findChatRoomsByUser(user);
    }
    
    public List<User> getChatRoomMembers(String userEmail, Integer roomId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ROOM_NOT_FOUND, "Chat room not found"));
        
        // Kiểm tra người dùng có phải là thành viên của phòng
        Optional<RoomMembers> memberCheck = roomMembersRepository.findByUserAndChatRoom(user, chatRoom);
        if (memberCheck.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS, "You are not a member of this chat room");
        }
        
        List<RoomMembers> roomMembers = roomMembersRepository.findByChatRoom(chatRoom);
        List<User> members = new ArrayList<>();
        for (RoomMembers member : roomMembers) {
            members.add(member.getUser());
        }
        
        return members;
    }
    
    @Transactional
    public GroupMessage sendGroupMessage(String senderEmail, GroupMessageRequest request) {
        try {
            log.info("Starting sendGroupMessage - User: {}, RoomId: {}", senderEmail, request.getRoomId());
            
            User sender = userRepository.findByEmail(senderEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Sender not found"));
            log.info("Sender found - ID: {}", sender.getId());
            
            ChatRoom chatRoom = chatRoomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new AppException(ErrorCode.CHAT_ROOM_NOT_FOUND, "Chat room not found"));
            log.info("Chat room found - ID: {}, Name: {}", chatRoom.getId(), chatRoom.getName());
            
            // Kiểm tra người dùng có phải là thành viên của phòng
            Optional<RoomMembers> memberCheck = roomMembersRepository.findByUserAndChatRoom(sender, chatRoom);
            if (memberCheck.isEmpty()) {
                log.warn("User {} is not a member of room {}", sender.getId(), chatRoom.getId());
                throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS, "You are not a member of this chat room");
            }
            log.info("User is a member of the room - Membership ID: {}", memberCheck.get().getId());
            
            GroupMessage message = new GroupMessage();
            message.setSender(sender);
            message.setChatRoom(chatRoom);
            message.setContent(request.getContent().trim());
            
            log.info("Saving message to database");
            GroupMessage savedMessage = groupMessageRepository.save(message);
            log.info("Message saved successfully - ID: {}", savedMessage.getId());
            
            // Gửi tin nhắn đến tất cả thành viên trong phòng qua WebSocket
            String destination = "/topic/chat/" + chatRoom.getId();
            log.info("Sending message to WebSocket destination: {}", destination);
            
            try {
                messagingTemplate.convertAndSend(destination, savedMessage);
                log.info("Message sent to WebSocket successfully");
            } catch (Exception e) {
                log.error("Error sending message to WebSocket: {}", e.getMessage(), e);
                // Không throw exception ở đây, vì message đã được lưu vào DB
            }
            
            return savedMessage;
        } catch (AppException e) {
            log.error("Application error in sendGroupMessage: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in sendGroupMessage: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Error sending group message: " + e.getMessage());
        }
    }
    
    public List<GroupMessage> getRoomMessages(String userEmail, Integer roomId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ROOM_NOT_FOUND, "Chat room not found"));
        
        // Kiểm tra người dùng có phải là thành viên của phòng
        Optional<RoomMembers> memberCheck = roomMembersRepository.findByUserAndChatRoom(user, chatRoom);
        if (memberCheck.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS, "You are not a member of this chat room");
        }
        
        return groupMessageRepository.findByChatRoomOrderByCreatedAtDesc(chatRoom);
    }
    
    @Transactional
    public void leaveRoom(String userEmail, Integer roomId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User not found"));
        
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(ErrorCode.CHAT_ROOM_NOT_FOUND, "Chat room not found"));
        
        // Tìm membership của người dùng trong phòng
        RoomMembers membership = roomMembersRepository.findByUserAndChatRoom(user, chatRoom)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED_ACCESS, "You are not a member of this chat room"));
        
        // Kiểm tra nếu là người tạo phòng và còn thành viên khác
        if (chatRoom.getCreatedBy().getId().equals(user.getId())) {
            List<RoomMembers> members = roomMembersRepository.findByChatRoom(chatRoom);
            if (members.size() > 1) {
                throw new AppException(ErrorCode.INVALID_OPERATION, "Room creator cannot leave the room with other members");
            }
            // Nếu chỉ còn mình người tạo, xóa phòng
            roomMembersRepository.delete(membership);
            chatRoomRepository.delete(chatRoom);
        } else {
            // Nếu không phải người tạo, chỉ xóa membership
            roomMembersRepository.delete(membership);
        }
    }
} 
package org.example.backend.repository;

import org.example.backend.entity.ChatRoom;
import org.example.backend.entity.RoomMembers;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMembersRepository extends JpaRepository<RoomMembers, Integer> {
    // Tìm tất cả các phòng mà người dùng là thành viên
    List<RoomMembers> findByUser(User user);
    
    // Tìm tất cả thành viên trong một phòng chat
    List<RoomMembers> findByChatRoom(ChatRoom chatRoom);
    
    // Kiểm tra xem một người dùng có phải là thành viên của phòng không
    Optional<RoomMembers> findByUserAndChatRoom(User user, ChatRoom chatRoom);
    
    // Lấy danh sách phòng chat mà một người dùng tham gia
    @Query("SELECT rm.chatRoom FROM RoomMembers rm WHERE rm.user = ?1")
    List<ChatRoom> findChatRoomsByUser(User user);
} 
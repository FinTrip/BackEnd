package org.example.backend.repository;

import org.example.backend.entity.ChatRoom;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Integer> {
    // Tìm phòng chat theo người tạo
    List<ChatRoom> findByCreatedBy(User user);
    
    // Tìm phòng chat theo tên (có thể dùng cho tìm kiếm)
    List<ChatRoom> findByNameContaining(String name);
} 
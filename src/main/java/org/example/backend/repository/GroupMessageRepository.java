package org.example.backend.repository;

import org.example.backend.entity.ChatRoom;
import org.example.backend.entity.GroupMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Integer> {
    // Tìm tin nhắn trong một phòng chat
    List<GroupMessage> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
    
    // Tìm tin nhắn mới nhất trong một phòng chat
    List<GroupMessage> findTop20ByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
} 
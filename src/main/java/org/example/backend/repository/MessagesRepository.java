package org.example.backend.repository;

import org.example.backend.entity.Messages;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessagesRepository extends JpaRepository<Messages, Integer> {
    // Tìm tin nhắn theo người nhận, sắp xếp theo thời gian giảm dần
    List<Messages> findByReceiverOrderByCreatedAtDesc(User receiver);
    
    // Tìm tin nhắn chưa đọc của người nhận
    List<Messages> findByReceiverAndIsReadFalseOrderByCreatedAtDesc(User receiver);
    
    // Tìm hội thoại giữa 2 người dùng
    List<Messages> findBySenderAndReceiverOrReceiverAndSenderOrderByCreatedAtDesc(
        User sender, User receiver, User receiver2, User sender2);
} 
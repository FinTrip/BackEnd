package org.example.backend.repository;

import org.example.backend.entity.Messages;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
        
    // Tìm tin nhắn mới nhất từ mỗi người gửi cho người nhận cụ thể
    @Query(value = 
        "SELECT m.* FROM messages m " +
        "INNER JOIN (" +
        "    SELECT MAX(id) as maxId " +
        "    FROM messages " +
        "    WHERE (sender_id != :userId AND receiver_id = :userId) OR (sender_id = :userId AND receiver_id != :userId) " +
        "    GROUP BY CASE " +
        "        WHEN sender_id = :userId THEN receiver_id " +
        "        ELSE sender_id " +
        "    END" +
        ") m2 ON m.id = m2.maxId " +
        "ORDER BY m.created_at DESC", nativeQuery = true)
    List<Messages> findLatestMessagesFromEachSender(@Param("userId") Integer userId);
} 
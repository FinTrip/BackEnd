package org.example.backend.repository;

import org.example.backend.entity.Friendship;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Integer> {
    // Tìm kiếm lời mời kết bạn giữa hai người dùng
    Optional<Friendship> findBySenderAndReceiver(User sender, User receiver);
    
    // Lấy tất cả lời mời kết bạn đã được chấp nhận của một người dùng
    List<Friendship> findBySenderAndStatus(User sender, Friendship.FriendshipStatus status);
    List<Friendship> findByReceiverAndStatus(User receiver, Friendship.FriendshipStatus status);
    
    // Kiểm tra xem hai người dùng có phải là bạn bè không
    boolean existsBySenderAndReceiverAndStatus(User sender, User receiver, Friendship.FriendshipStatus status);
    boolean existsByReceiverAndSenderAndStatus(User receiver, User sender, Friendship.FriendshipStatus status);
} 
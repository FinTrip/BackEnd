package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@ToString(exclude = {"role", "travelGroups", "travelPlans", "notifications", "recommendations", "issueReports", "sentFriendRequests", "receivedFriendRequests"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "wallet_balance", nullable = false)
    private Long walletBalance = 0L;

    @ManyToOne
    @JoinColumn(name = "role_id")
    @JsonManagedReference
    private Role role;

    @OneToMany(mappedBy = "user")
    private List<TravelGroup> travelGroups;

    @OneToMany(mappedBy = "user")
    private List<TravelPlan> travelPlans;

    @OneToMany(mappedBy = "user")
    private List<Notification> notifications;

//    @OneToMany(mappedBy = "user")
//    private List<Recommendation> recommendations;

    @OneToMany(mappedBy = "sender")
    private List<Friendship> sentFriendRequests;

    @OneToMany(mappedBy = "receiver")
    private List<Friendship> receivedFriendRequests;

    private java.time.LocalDateTime vipExpireAt;

    public enum UserStatus {
        active, inactive, banned
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (walletBalance == null) {
            walletBalance = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (walletBalance == null) {
            walletBalance = 0L;
        }
    }
    
    // Phương thức để lấy danh sách bạn bè đã được chấp nhận
    @JsonIgnore
    public List<User> getFriends() {
        List<User> friends = new ArrayList<>();
        
        // Lấy người nhận từ danh sách lời mời gửi đi và đã được chấp nhận
        if (sentFriendRequests != null) {
            sentFriendRequests.stream()
                .filter(f -> f.getStatus() == Friendship.FriendshipStatus.ACCEPTED)
                .forEach(f -> friends.add(f.getReceiver()));
        }
        
        // Lấy người gửi từ danh sách lời mời nhận được và đã được chấp nhận
        if (receivedFriendRequests != null) {
            receivedFriendRequests.stream()
                .filter(f -> f.getStatus() == Friendship.FriendshipStatus.ACCEPTED)
                .forEach(f -> friends.add(f.getSender()));
        }
        
        return friends;
    }
    
    // Phương thức để lấy danh sách lời mời kết bạn đang chờ xác nhận
    @JsonIgnore
    public List<User> getPendingFriendRequests() {
        if (receivedFriendRequests == null) {
            return new ArrayList<>();
        }
        
        return receivedFriendRequests.stream()
            .filter(f -> f.getStatus() == Friendship.FriendshipStatus.PENDING)
            .map(Friendship::getSender)
            .collect(Collectors.toList());
    }
} 
package org.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.entity.Friendship;
import org.example.backend.entity.User;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendDto {
    private Integer id;
    private Integer friendshipId;
    private String fullName;
    private String email;
    private LocalDateTime friendsSince;
    
    public static FriendDto fromFriendship(Friendship friendship, String currentUserEmail) {
        User friend;
        
        // Xác định người bạn (không phải người dùng hiện tại)
        if (friendship.getSender().getEmail().equals(currentUserEmail)) {
            friend = friendship.getReceiver();
        } else {
            friend = friendship.getSender();
        }
        
        return FriendDto.builder()
                .id(friend.getId())
                .friendshipId(friendship.getId())
                .fullName(friend.getFullName())
                .email(friend.getEmail())
                .friendsSince(friendship.getUpdatedAt()) // Thời điểm chấp nhận kết bạn
                .build();
    }
} 
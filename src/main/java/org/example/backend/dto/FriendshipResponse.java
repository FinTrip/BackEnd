package org.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.entity.Friendship;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {
    private Integer id;
    private UserSummary sender;
    private UserSummary receiver;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Integer id;
        private String fullName;
        private String email;
    }
    
    public static FriendshipResponse fromEntity(Friendship friendship) {
        return FriendshipResponse.builder()
                .id(friendship.getId())
                .sender(UserSummary.builder()
                        .id(friendship.getSender().getId())
                        .fullName(friendship.getSender().getFullName())
                        .email(friendship.getSender().getEmail())
                        .build())
                .receiver(UserSummary.builder()
                        .id(friendship.getReceiver().getId())
                        .fullName(friendship.getReceiver().getFullName())
                        .email(friendship.getReceiver().getEmail())
                        .build())
                .status(friendship.getStatus().name())
                .createdAt(friendship.getCreatedAt())
                .updatedAt(friendship.getUpdatedAt())
                .build();
    }
} 
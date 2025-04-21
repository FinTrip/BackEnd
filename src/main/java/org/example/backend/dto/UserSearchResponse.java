package org.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend.entity.User;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private Integer id;
    private String email;
    private String fullName;
    private boolean alreadyFriend;
    private boolean requestPending;
    
    public static UserSearchResponse fromUser(User user, boolean alreadyFriend, boolean requestPending) {
        return UserSearchResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .alreadyFriend(alreadyFriend)
                .requestPending(requestPending)
                .build();
    }
} 
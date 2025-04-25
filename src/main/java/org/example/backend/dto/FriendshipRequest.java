package org.example.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
 
@Data
public class FriendshipRequest {
    @NotNull(message = "ID người nhận không được để trống")
    private Integer receiverId;
} 
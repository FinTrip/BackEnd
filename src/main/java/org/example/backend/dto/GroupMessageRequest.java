package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupMessageRequest {
    @NotNull(message = "Room ID is required")
    private Integer roomId;

    @NotBlank(message = "Message content cannot be empty")
    private String content;
} 
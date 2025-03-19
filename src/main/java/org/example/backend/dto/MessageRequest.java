package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageRequest {
    @NotNull(message = "Receiver ID is required")
    private Integer receiverId;

    @NotBlank(message = "Message content cannot be empty")
    private String content;
} 
package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ChatRoomRequest {
    @NotBlank(message = "Room name cannot be empty")
    private String name;
    
    @NotEmpty(message = "At least one member must be added")
    private List<Integer> memberIds;
} 
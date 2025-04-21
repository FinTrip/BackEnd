package org.example.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddMemberRequest {
    @NotNull(message = "Room ID is required")
    private Integer roomId;
    
    @NotEmpty(message = "Member IDs cannot be empty")
    private List<Integer> memberIds;
} 
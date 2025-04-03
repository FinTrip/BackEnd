package org.example.backend.dto;

import lombok.Data;

@Data
public class ReplyRequest {
    private String content;
    private Integer commentId;  // Sử dụng Integer để phù hợp với Comments.id
} 
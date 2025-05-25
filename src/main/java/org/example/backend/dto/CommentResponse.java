package org.example.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Integer id;
    private Long postId ;
    private String content ;
    private Integer authorId ;
    private LocalDateTime createdAt  ;
}

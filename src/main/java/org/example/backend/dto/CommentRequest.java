package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentRequest {
    @NotNull(message = "Post ID is required")  // Validation: postId không được null
    private Integer postId;

    @NotBlank(message = "Content is required")  // Validation: content không được trống
    private String content;
}


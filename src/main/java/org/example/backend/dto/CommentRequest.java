package org.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRequest {
    @NotNull(message = "Post ID is required")  // Validation: postId không được null
    private Integer postId;

    @NotBlank(message = "Content is required")  // Validation: content không được trống
    @Size(min = 1, max = 1000, message = "Comment length must be between 1 and 1000 characters")
    private String content;
}


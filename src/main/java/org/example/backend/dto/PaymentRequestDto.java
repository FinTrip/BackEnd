package org.example.backend.dto;

import lombok.Data;

@Data
public class PaymentRequestDto {
    private Long amount;
    private String description;
    private String returnUrl;
    private String cancelUrl;
    private String type;
    private Integer duration;
    private Long userId;
    private Long postId;
} 
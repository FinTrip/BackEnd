package org.example.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
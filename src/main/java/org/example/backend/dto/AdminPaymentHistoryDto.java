package org.example.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.backend.entity.Payment;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminPaymentHistoryDto {
    private Integer userId;
    private String userEmail;
    private LocalDateTime createdAt;
    private String status;
    private Long amount;
    private String description;
    private String payosOrderId;
    
    public static AdminPaymentHistoryDto fromEntity(Payment payment) {
        return AdminPaymentHistoryDto.builder()
                .userId(payment.getUser() != null ? payment.getUser().getId() : null)
                .userEmail(payment.getUser() != null ? payment.getUser().getEmail() : null)
                .createdAt(payment.getCreatedAt())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .description(payment.getDescription())
                .payosOrderId(payment.getPayosOrderId())
                .build();
    }
} 
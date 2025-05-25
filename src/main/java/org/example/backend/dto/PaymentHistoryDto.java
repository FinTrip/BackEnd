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
public class PaymentHistoryDto {
    private LocalDateTime createdAt;
    private String status;
    private Long amount;
    private String description;
    private String payosOrderId;
    
    public static PaymentHistoryDto fromEntity(Payment payment) {
        return PaymentHistoryDto.builder()
                .createdAt(payment.getCreatedAt())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .description(payment.getDescription())
                .payosOrderId(payment.getPayosOrderId())
                .build();
    }
} 
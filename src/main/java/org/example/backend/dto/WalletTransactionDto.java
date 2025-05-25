package org.example.backend.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.backend.entity.WalletTransaction;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDto {
    private Long id;
    private Long amount;
    private String type;
    private String description;
    private LocalDateTime createdAt;
    private String formattedCreatedAt;
    private Integer userId;
    private String userEmail;
    private String userName;
    private String paymentType;
    private String paymentStatus;
    private String paymentId;
    
    public static WalletTransactionDto fromEntity(WalletTransaction transaction) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        
        WalletTransactionDto dto = WalletTransactionDto.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .type(transaction.getType().name())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .formattedCreatedAt(transaction.getCreatedAt() != null ? 
                        transaction.getCreatedAt().format(formatter) : null)
                .build();
        
        // Add user info if available
        if (transaction.getUser() != null) {
            dto.setUserId(transaction.getUser().getId());
            dto.setUserEmail(transaction.getUser().getEmail());
            dto.setUserName(transaction.getUser().getFullName());
        }
        
        // Add payment info if available
        if (transaction.getPayment() != null) {
            dto.setPaymentType(transaction.getPayment().getType());
            dto.setPaymentStatus(transaction.getPayment().getStatus());
            dto.setPaymentId(transaction.getPayment().getPayosOrderId());
        }
        
        return dto;
    }
}
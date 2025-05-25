package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Long amount; // Positive for deposits, negative for withdrawals
    
    @Enumerated(EnumType.STRING)
    private TransactionType type;
    
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "payment_id")
    private Payment payment; // Related payment if applicable
    
    private LocalDateTime createdAt;
    
    public enum TransactionType {
        DEPOSIT, // Nạp tiền vào ví
        WITHDRAWAL, // Rút tiền từ ví
        PURCHASE_VIP, // Mua VIP
        PURCHASE_AD // Mua quảng cáo
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 
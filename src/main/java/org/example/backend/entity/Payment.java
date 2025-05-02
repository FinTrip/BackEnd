package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long amount;
    private String description;
    private String status; // PENDING, SUCCESS, FAILED
    private String payosOrderId;
    private String payosCheckoutUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String type; // "VIP" hoặc "AD"
    private Integer duration; // Số tháng (1, 3, 6), chỉ dùng cho quảng cáo hoặc VIP
    private Long userId; // id user thực hiện giao dịch
    private Long postId; // id bài viết quảng cáo (nếu có)
} 
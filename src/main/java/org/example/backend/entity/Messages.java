    package org.example.backend.entity;

    import jakarta.persistence.*;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    import lombok.AllArgsConstructor;

    import java.time.LocalDateTime;

    @Data
    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Table(name = "messages")
    public class Messages {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        @ManyToOne
        @JoinColumn(name = "sender_id")
        private User sender;  // Người gửi

        @ManyToOne
        @JoinColumn(name = "receiver_id")
        private User receiver;  // Người nhận

        @Column(columnDefinition = "TEXT")
        private String content;

        @Column(name = "is_read")
        private Boolean isRead = false;  // Trạng thái đã đọc

        @Column(name = "created_at")
        private LocalDateTime createdAt;

        @PrePersist
        protected void onCreate() {
            createdAt = LocalDateTime.now();
            if (isRead == null) {
                isRead = false;
            }
        }
    }

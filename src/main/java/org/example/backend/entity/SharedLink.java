package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "sharedlinks")
public class SharedLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(name = "share_link", unique = true)
    private String shareLink;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
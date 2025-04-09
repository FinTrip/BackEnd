package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.awt.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name ="blog_post")
public class BlogPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "travel_plan_id")
    private TravelPlan travelPlan;

    @Column(columnDefinition = "TEXT", length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PostStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "images")
    private String images;
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    public enum PostStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }
    @Column(name = "views")
    private Integer views = 0;

    @Column(name = "likes")
    private Integer likes = 0;

    @Column(name = "comments_count")
    private Integer commentsCount = 0;

    @Column(name = "liked_user_emails", columnDefinition = "TEXT")
    private String likedUserEmails = "";


}

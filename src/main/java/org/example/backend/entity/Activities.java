package org.example.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "activities")
public class Activities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer activityId;

    @ManyToOne
    @JoinColumn(name = "id_TralvelPlan")
    @JsonIgnore
    private TravelPlan travelPlan;

    @ManyToOne
    @JoinColumn(name = "id_service")
    @JsonIgnore
    private Services service;

    @Column(name = "name_services")
    private String nameservice;

    @Column(name = "address")
    private String address;

    @Column(name = "price")
    private float price;

    @Column(name = "image")
    private String image;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

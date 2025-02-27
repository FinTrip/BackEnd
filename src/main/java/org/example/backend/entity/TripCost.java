package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "trip_costs")
public class TripCost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "plan_id")
    private TravelPlan plan;

    @Column(name = "accommodation_cost", precision = 10, scale = 2)
    private BigDecimal accommodationCost;

    @Column(name = "food_cost", precision = 10, scale = 2)
    private BigDecimal foodCost;

    @Column(name = "transport_cost", precision = 10, scale = 2)
    private BigDecimal transportCost;

    @Column(name = "activities_cost", precision = 10, scale = 2)
    private BigDecimal activitiesCost;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;
}
package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
@Table(name = "days")
public class Day {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(name = "day_index")
    private Integer dayIndex;

    @Column(name = "date_str")
    private String dateStr;

    @OneToMany(mappedBy = "day", cascade = CascadeType.ALL)
    private List<Itinerary> itineraries;
}
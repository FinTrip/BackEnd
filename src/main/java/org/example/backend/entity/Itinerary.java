package org.example.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "itineraries")
public class Itinerary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "day_id")
    private Day day;

    private String timeslot;

    @Column(name = "food_title")
    private String foodTitle;

    @Column(name = "food_rating")
    private Float foodRating;

    @Column(name = "food_price")
    private String foodPrice;

    @Column(name = "food_address")
    private String foodAddress;

    @Column(name = "food_phone")
    private String foodPhone;

    @Column(name = "food_link")
    private String foodLink;

    @Column(name = "food_image")
    private String foodImage;

    @Column(name = "food_time")
    private String food_time;

    @Column(name = "place_title")
    private String placeTitle;

    @Column(name = "place_rating")
    private Float placeRating;

    @Column(name = "place_description", columnDefinition = "TEXT")
    private String placeDescription;

    @Column(name = "place_address")
    private String placeAddress;

    @Column(name = "place_img")
    private String placeImg;

    @Column(name = "place_link")
    private String placeLink;

    @Column(name = "place_time")
    private String place_time;

    @Column(name = "`order`")
    private Integer order;

    @ManyToOne
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;
}

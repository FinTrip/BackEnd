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

    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "hotel_link")
    private String hotelLink;

    @Column(name = "hotel_description", columnDefinition = "TEXT")
    private String hotelDescription;

    @Column(name = "hotel_price")
    private String hotelPrice;

    @Column(name = "hotel_class")
    private String hotelClass;

    @Column(name = "hotel_img_origin")
    private String hotelImgOrigin;

    @Column(name = "hotel_location_rating")
    private Float hotelLocationRating;

    @Column(name = "`order`")
    private Integer order;
}

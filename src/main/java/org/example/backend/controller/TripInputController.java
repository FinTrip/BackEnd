package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.TripInput;
import org.example.backend.dto.TripResponse;
import org.example.backend.dto.DestinationResponse;
import org.example.backend.entity.Destination;
import org.example.backend.entity.TravelPlan;
import org.example.backend.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trip")
@RequiredArgsConstructor
public class TripInputController {
    private final TripService tripService;

    @PostMapping("/input")
    public ResponseEntity<?> processTripInput(@RequestBody TripInput request) {
        List<Destination> destinations = tripService.processTripInput(request);

        List<DestinationResponse> destinationResponses = destinations.stream()
            .map(dest -> DestinationResponse.builder()
                .id(dest.getId())
                .name(dest.getName())
                .description(dest.getDescription())
                .avgCost(dest.getAvgCost())
                .recommendedDays(dest.getRecommendedDays())
                .build())
            .collect(Collectors.toList());

        TripResponse response = TripResponse.builder()
            .id(destinations.get(0).getId())
            .startDate(destinations.get(0).getCreatedAt())
            .endDate(destinations.get(0).getUpdatedAt())
            .totalBudget(BigDecimal.valueOf(request.getBudget()))
            .isSaved(true)
            .destinations(destinationResponses)
            .build();

        return ResponseEntity.ok(response);
    }
}
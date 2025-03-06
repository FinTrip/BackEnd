package org.example.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.TripInput;
import org.example.backend.entity.*;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;
import org.example.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripService {
    private final LocationRepository locationRepository;
    private final DestinationRepository destinationRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final TripCostRepository tripCostRepository;

    @Transactional
    public List<Destination> processTripInput(TripInput request) {
        log.info("Processing trip input for {} people, {} days in {}", 
            request.getPeople(), request.getDay(), request.getLocation());

        // Tìm location
        Location location = locationRepository.findByName(request.getLocation())
            .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));

        // Tính ngân sách cho mỗi ngày (trừ đi chi phí cố định)
        BigDecimal fixedCosts = calculateFixedCosts(request);
        BigDecimal remainingBudget = BigDecimal.valueOf(request.getBudget()).subtract(fixedCosts);
        BigDecimal dailyBudget = remainingBudget.divide(BigDecimal.valueOf(request.getDay()), 2, BigDecimal.ROUND_DOWN);
        
        log.info("Daily budget for destinations: {}", dailyBudget);
        
        // Tìm tất cả destinations phù hợp với ngân sách
        List<Destination> destinations = destinationRepository.findByLocationAndAvgCostLessThan(
            location, 
            dailyBudget
        );

        log.info("Found {} destinations within budget", destinations.size());
        destinations.forEach(dest -> 
            log.info("Destination: {}, Cost: {}", dest.getName(), dest.getAvgCost())
        );

        if (destinations.isEmpty()) {
            throw new AppException(ErrorCode.DESTINATION_NOT_FOUND);
        }

        // Sắp xếp destinations theo chi phí giảm dần
        destinations.sort((d1, d2) -> d2.getAvgCost().compareTo(d1.getAvgCost()));

        // Tạo travel plan
        TravelPlan travelPlan = new TravelPlan();
        travelPlan.setStartDate(LocalDateTime.now());
        travelPlan.setEndDate(LocalDateTime.now().plusDays(request.getDay()));
        travelPlan.setTotalBudget(BigDecimal.valueOf(request.getBudget()));
        travelPlan.setIsSaved(true);
        travelPlan = travelPlanRepository.save(travelPlan);

        // Tính toán chi phí
        TripCost tripCost = new TripCost();
        tripCost.setPlan(travelPlan);
        tripCost.setAccommodationCost(calculateAccommodationCost(request));
        tripCost.setFoodCost(calculateFoodCost(request));
        tripCost.setTransportCost(calculateTransportCost(request));
        tripCost.setActivitiesCost(calculateActivitiesCost(request));
        tripCost.setTotalCost(BigDecimal.valueOf(request.getBudget()));
        tripCostRepository.save(tripCost);

        return destinations;
    }

    private BigDecimal calculateFixedCosts(TripInput request) {
        return calculateAccommodationCost(request)
            .add(calculateFoodCost(request))
            .add(calculateTransportCost(request))
            .add(calculateActivitiesCost(request));
    }

    private BigDecimal calculateAccommodationCost(TripInput request) {
        // Chi phí khách sạn: 800k/người/ngày (phòng đôi)
        return BigDecimal.valueOf(request.getPeople() * request.getDay() * 800000);
    }

    private BigDecimal calculateFoodCost(TripInput request) {
        // Chi phí ăn uống: 300k/người/ngày (3 bữa + đồ uống)
        return BigDecimal.valueOf(request.getPeople() * request.getDay() * 300000);
    }

    private BigDecimal calculateTransportCost(TripInput request) {
        // Chi phí di chuyển: 200k/người/ngày (taxi + xe buýt)
        return BigDecimal.valueOf(request.getPeople() * request.getDay() * 200000);
    }

    private BigDecimal calculateActivitiesCost(TripInput request) {
        // Chi phí hoạt động: 200k/người/ngày (vé tham quan + hoạt động)
        return BigDecimal.valueOf(request.getPeople() * request.getDay() * 200000);
    }
}


package org.example.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TripResponse {
    private Integer id;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalBudget;
    private Boolean isSaved;
    private List<DestinationResponse> destinations;
}

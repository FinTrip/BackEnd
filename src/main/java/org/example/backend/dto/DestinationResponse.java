package org.example.backend.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class DestinationResponse {
    private Integer id;
    private String name;
    private String description;
    private BigDecimal avgCost;
    private Integer recommendedDays;
} 
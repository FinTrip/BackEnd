package org.example.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LocationResponse {
    private Integer id;
    private String name;
    private String country;
    private BigDecimal latitude;
    private BigDecimal longitude;
}

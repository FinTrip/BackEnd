package org.example.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TripInput {
   @Min(value = 1, message = "so nguoi phai lon hon 1")
   private int people;
   @Min(value = 1, message = "So ngay phai lon hon 0")
   private int day;
   @NotBlank(message = "Vui long dien mua")
   private String season;
   @Min(value = 0, message = "Ngan sach khong duoc am")
   private double budget;
   @NotBlank(message = "Vui long dien dia diem")
   private String location;
}

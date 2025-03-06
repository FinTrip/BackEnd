package org.example.backend.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Size;

@Data
@Getter
@Setter
public class UpdateRequest {
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;
    
    @Size(min = 8, message = "Current password must be at least 8 characters")
    private String currentPassword;
    
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;
}

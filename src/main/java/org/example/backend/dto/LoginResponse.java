package org.example.backend.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
@Getter
@Setter
public class LoginResponse {
    private String token;
    private int id;
    private String email;
    private String fullName;
    private String role;
}
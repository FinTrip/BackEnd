package org.example.backend.controller;

import org.example.backend.dto.*;
import org.example.backend.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Login failed", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterRequest>> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            ApiResponse<RegisterRequest> response = new ApiResponse<>();
            response.setCode(200);
            response.setMessage("User registered successfully");
            response.setResult(request);
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Registration failed", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request);
            ApiResponse<String> response = new ApiResponse<>();
            response.setCode(200);
            response.setMessage("Password reset email sent.");
            response.setResult(null); // No specific result to return
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Forgot password failed", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            ApiResponse<String> response = new ApiResponse<>();
            response.setCode(200);
            response.setMessage("Password reset successfully.");
            response.setResult(null); // No specific result to return
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Reset password failed", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<String>> updateUser(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateRequest request) {
        try {
            log.info("Start update user process with token");
            
            // Remove "Bearer " prefix if present
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            authService.updateUserByToken(token, request);
            
            ApiResponse<String> response = new ApiResponse<>();
            response.setCode(200);
            response.setMessage("User updated successfully");
            return ResponseEntity.ok(response);
        } catch (AppException e) {
            log.error("Update user failed with AppException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Update user failed with unexpected error: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}

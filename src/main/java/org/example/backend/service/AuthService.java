package org.example.backend.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.LoginRequest;
import org.example.backend.dto.LoginResponse;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.backend.dto.RegisterRequest;
import org.example.backend.entity.Role;
import org.example.backend.repository.RoleRepository;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;

    public LoginResponse login(LoginRequest request) {
        try {
            log.info("Start login process for email: {}", request.getEmail());
            validateLoginRequest(request);

            log.info("Finding user with email: {}", request.getEmail());
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AppException(
                        ErrorCode.USER_NOT_FOUND, 
                        "User not found with email: " + request.getEmail()
                    ));
            log.info("Found user: {}", user);
                
            log.info("Checking password for user: {}", user.getEmail());
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.error("Invalid password for user: {}", user.getEmail());
                throw new AppException(ErrorCode.INVALID_CREDENTIALS);
            }

            log.info("Password matched for user: {}", user.getEmail());
            
            // Kiểm tra role
            if (user.getRole() == null) {
                log.error("User role is null for user: {}", user.getEmail());
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User role not found");
            }
            log.info("User role: {}", user.getRole().getRoleName());

            log.info("Generating token for user: {}", user.getEmail());
            String token;
            try {
                token = jwtService.generateToken(user);
                log.info("Token generated successfully");
            } catch (Exception e) {
                log.error("Failed to generate token: ", e);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate token");
            }
            
            log.info("Building login response for user: {}", user.getEmail());
            try {
                return LoginResponse.builder()
                        .token(token)
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .role(user.getRole().getRoleName())
                        .build();
            } catch (Exception e) {
                log.error("Failed to build login response: ", e);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to build response");
            }
        } catch (AppException e) {
            log.error("Login failed with AppException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Login failed with unexpected error: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Unexpected error during login");
        }
    }

    public void register(RegisterRequest request) {
        validateRegisterRequest(request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setStatus(User.UserStatus.active);

        Role userRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Default role not found"));
        user.setRole(userRole);

        try {
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Registration failed", e);
            throw new AppException(ErrorCode.DATABASE_ERROR);
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Login request cannot be null");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Password is required");
        }
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Register request cannot be null");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Password is required");
        }
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Full name is required");
        }
        if (!isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.INVALID_FORMAT, "Invalid email format");
        }
        if (!isValidPassword(request.getPassword())) {
            throw new AppException(ErrorCode.INVALID_FORMAT, "Password must be at least 8 characters long");
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }
}

package org.example.backend.service;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.dto.LoginRequest;
import org.example.backend.dto.LoginResponse;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.backend.dto.RegisterRequest;
import org.example.backend.entity.Role;
import org.example.backend.repository.RoleRepository;
import org.example.backend.exception.UserNotFoundException;
import org.example.backend.exception.InvalidCredentialsException;
import org.example.backend.exception.InvalidInputException;
import org.example.backend.exception.UserAlreadyExistsException;
import org.example.backend.exception.RoleNotFoundException;
import org.example.backend.exception.RegistrationFailedException;

@Service
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;

    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JwtService jwtService,
                      RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.roleRepository = roleRepository;
    }

    public LoginResponse login(LoginRequest request) {
        // Validate input
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Email and password cannot be null");
        }

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));
            
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = jwtService.generateToken(user);
        
        return LoginResponse.builder()
            .token(token)
            .email(user.getEmail())
            .fullName(user.getFullName())
            .role(user.getRole().getRoleName())
            .build();
    }

    public void register(RegisterRequest request) {
        // Validate input
        if (request.getEmail() == null || request.getPassword() == null || request.getFullName() == null) {
            throw new IllegalArgumentException("All fields are required");
        }

        // Validate email format
        if (!isValidEmail(request.getEmail())) {
            throw new InvalidInputException("Invalid email format");
        }

        // Validate password strength
        if (!isValidPassword(request.getPassword())) {
            throw new InvalidInputException("Password must be at least 8 characters long");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setStatus(User.UserStatus.active);

        Role userRole = roleRepository.findByRoleName("USER")
            .orElseThrow(() -> new RoleNotFoundException("Default role not found"));
        user.setRole(userRole);

        try {
            userRepository.save(user);
        } catch (Exception e) {
            log.error("Error saving user", e);
            throw new RegistrationFailedException("Failed to register user");
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 8;
    }
}

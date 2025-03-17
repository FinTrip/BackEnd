package org.example.backend.service;

import com.sun.source.doctree.UsesTree;
import org.example.backend.dto.*;
import org.example.backend.entity.PasswordResetToken;
import org.example.backend.repository.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend.entity.User;
import org.example.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.backend.entity.Role;
import org.example.backend.repository.RoleRepository;
import org.example.backend.exception.AppException;
import org.example.backend.exception.ErrorCode;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final JavaMailSender mailSender;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public LoginResponse login(LoginRequest request) {
        try {
            log.info("Attempting login for user: {}", request.getEmail());
            
            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
            
            log.info("Found user: {}", user);
            log.info("Checking password for user: {}", user.getEmail());
            
            // So sánh mật khẩu đã mã hóa
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                log.error("Invalid password for user: {}", user.getEmail());
                throw new AppException(ErrorCode.INVALID_CREDENTIALS);
            }

            // Tạo token nếu mật khẩu đúng
            String token = jwtService.generateToken(user);
            
            log.info("Login successful for user: {}", user.getEmail());
            return LoginResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole() != null ? user.getRole().getRoleName() : "USER")
                    .build();

        } catch (AppException e) {
            log.error("Login failed with AppException: {} ", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Login failed with unexpected error", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public void register(RegisterRequest request) {
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // Mã hóa mật khẩu trước khi lưu
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Lấy role mặc định (USER)
        Role defaultRole = roleRepository.findByRoleName("USER")
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND, "Default role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .fullName(request.getFullName())
                .status(User.UserStatus.active)
                .role(defaultRole)
                .build();

        userRepository.save(user);
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

    public void forgotPassword(ForgotPasswordRequest request) {
        try {
            log.info("Starting forgot password process for email: {}", request.getEmail());
            
            // Validate email
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Email không được để trống");
            }

            // Find user
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new AppException(ErrorCode.FORGOT_PASSWORD_USER_NOT_FOUND));
            log.info("Found user: {}", user.getEmail());

            // Delete existing token if any
            try {
                passwordResetTokenRepository.findByUser(user)
                        .ifPresent(token -> {
                            log.info("Deleting existing token for user: {}", user.getEmail());
                            passwordResetTokenRepository.delete(token);
                        });
            } catch (Exception e) {
                log.error("Error deleting existing token: ", e);
            }

            // Create new token
            String token = UUID.randomUUID().toString();
            PasswordResetToken passwordResetToken = new PasswordResetToken(token, user);
            passwordResetToken.setExpiryDate(java.time.LocalDateTime.now().plusMinutes(30));
            
            try {
                passwordResetTokenRepository.save(passwordResetToken);
                log.info("Saved new token for user: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Error saving token: ", e);
                throw new AppException(ErrorCode.DATABASE_ERROR, "Lỗi khi lưu token: " + e.getMessage());
            }

            // Send email
            try {
                sendPasswordResetEmail(user.getEmail(), token);
                log.info("Successfully sent reset email to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Error sending email: ", e);
                throw new AppException(ErrorCode.FORGOT_PASSWORD_EMAIL_NOT_SENT, 
                    "Lỗi khi gửi email: " + e.getMessage());
            }
        } catch (AppException e) {
            log.error("Forgot password failed with AppException: ", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in forgot password: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, 
                "Lỗi không mong muốn: " + e.getMessage());
        }
    }

    private void sendPasswordResetEmail(String email, String token) {
        try {
            String resetLink = "http://localhost:8081/identity/reset-password?token=" + token;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Yêu cầu đặt lại mật khẩu");
            message.setText("Xin chào,\n\n" +
                    "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.\n" +
                    "Vui lòng nhấp vào liên kết bên dưới để đặt lại mật khẩu:\n\n" +
                    resetLink + "\n\n" +
                    "Liên kết này sẽ hết hạn sau 30 phút.\n" +
                    "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                    "Trân trọng,\n" +
                    "Đội ngũ hỗ trợ");
            
            mailSender.send(message);
            log.info("Email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send email: ", e);
            throw new AppException(ErrorCode.FORGOT_PASSWORD_EMAIL_NOT_SENT, 
                "Không thể gửi email: " + e.getMessage());
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing reset password for token: {}", request.getToken());

        PasswordResetToken token = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.RESET_PASSWORD_INVALID_TOKEN));

        // Kiểm tra token hết hạn
        if (token.getExpiryDate().isBefore(java.time.LocalDateTime.now())) {
            passwordResetTokenRepository.delete(token);
            throw new AppException(ErrorCode.RESET_PASSWORD_TOKEN_EXPIRED, "Token đã hết hạn");
        }

        User user = token.getUser();
        if (user == null) {
            log.error("User not found for token: {}", request.getToken());
            throw new AppException(ErrorCode.RESET_PASSWORD_INVALID_TOKEN, "Không tìm thấy người dùng cho token này");
        }

        // Validate mật khẩu mới
        if (!isValidPassword(request.getNewPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT, 
                "Mật khẩu mới phải có ít nhất 8 ký tự");
        }

        // Kiểm tra mật khẩu mới không trùng với mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT, 
                "Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password reset successfully for user: {}", user.getEmail());

        // Xóa token sau khi sử dụng
        passwordResetTokenRepository.delete(token);
        log.info("Password reset token deleted for token: {}", request.getToken());
    }

    public void updateUserByToken(String token, UpdateRequest request) {
        try {
            log.info("Start update user process with token");
            
            // Validate request
            if (request == null) {
                log.error("Update request is null");
                throw new AppException(ErrorCode.INVALID_INPUT, "Update request cannot be null");
            }
            
            // Validate token
            if (token == null || token.trim().isEmpty()) {
                log.error("Token is missing");
                throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Authorization token is required");
            }
            
            // Get user from token
            User user = jwtService.extractUser(token);
            if (user == null) {
                log.error("Invalid token: no user found");
                throw new AppException(ErrorCode.INVALID_TOKEN, "Invalid token");
            }
            log.info("Found user: {}", user.getEmail());
            
            // 2 Update Ten neu co
            if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
                String newFullName = request.getFullName().trim();
                if (newFullName.length() < 2 || newFullName.length() > 100) {
                    log.error("Invalid full name length: {}", newFullName.length());
                    throw new AppException(ErrorCode.INVALID_FORMAT, "Full name must be between 2 and 100 characters");
                }
                log.info("Updating fullName from {} to {}", user.getFullName(), newFullName);
                user.setFullName(newFullName);
            }
            
            //3 Update password
            if (request.getNewPassword() != null && !request.getNewPassword().trim().isEmpty()) {
                log.info("Updating password for user: {}", user.getEmail());
                
                // Kiểm tra mật khẩu hiện tại có được cung cấp không
                if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
                    log.error("Current password is missing");
                    throw new AppException(ErrorCode.MISSING_REQUIRED_FIELD, "Current Password is required");
                }

                // Kiểm tra mật khẩu hiện tại có đúng không
                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                    log.error("Current password does not match");
                    throw new AppException(ErrorCode.PASSWORD_MISMATCH);
                }

                // Validate mật khẩu mới
                String newPassword = request.getNewPassword().trim();
                if (newPassword.length() < 8) {
                    log.error("New password is too short");
                    throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT, "New password must be at least 8 characters");
                }

                // Kiểm tra mật khẩu mới không trùng với mật khẩu cũ
                if (passwordEncoder.matches(newPassword, user.getPassword())) {
                    log.error("New password is same as current password");
                    throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT, "New password must be different from current password");
                }

                // Cập nhật mật khẩu mới
                user.setPassword(passwordEncoder.encode(newPassword));
                log.info("Password updated successfully");
            }
            
            // 4. Lưu thay đổi
            try {
                userRepository.save(user);
                log.info("User updated successfully");
            } catch (Exception e) {
                log.error("Failed to save user: {}", e.getMessage(), e);
                throw new AppException(ErrorCode.DATABASE_ERROR, "Failed to save user: " + e.getMessage());
            }
        } catch (AppException e) {
            log.error("Update user failed with AppException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Update user failed with unexpected error: ", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Unexpected error: " + e.getMessage());
        }
    }

}

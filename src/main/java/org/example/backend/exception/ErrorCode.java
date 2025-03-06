package org.example.backend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // Auth related errors
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Invalid input provided"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "User already exists"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    MISSING_REQUIRED_FIELD(HttpStatus.BAD_REQUEST, "Missing required field"),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "Invalid format"),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "User account is inactive"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token has expired"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token"),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "Unauthorized access"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "Password does not match"),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "Invalid password format"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "Invalid email format"),
    
    // Role related errors
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "Role not found"),
    INVALID_ROLE(HttpStatus.BAD_REQUEST, "Invalid role"),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "Insufficient permissions"),
    // Forgot Password related errors
    FORGOT_PASSWORD_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với email này"),
    FORGOT_PASSWORD_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token đặt lại mật khẩu đã hết hạn"),
    FORGOT_PASSWORD_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token đặt lại mật khẩu không hợp lệ"),
    FORGOT_PASSWORD_EMAIL_NOT_SENT(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể gửi email đặt lại mật khẩu"),
    //Question input
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy địa điểm"),
    DESTINATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy điểm đến phù hợp với ngân sách"),
    BUDGET_TOO_LOW(HttpStatus.BAD_REQUEST, "Ngân sách không đủ cho các địa điểm đã chọn"),

    // Reset Password related errors
    RESET_PASSWORD_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc đã hết hạn"),
    RESET_PASSWORD_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token đã hết hạn"),
    RESET_PASSWORD_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "Mật khẩu mới và xác nhận không khớp"),
    RESET_PASSWORD_INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "Mật khẩu mới không đáp ứng yêu cầu định dạng"),
    RESET_PASSWORD_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu cũ"),

    // General errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Database error occurred"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access forbidden"),
    
    // API related errors
    API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "API error occurred"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Service is currently unavailable"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "Request timeout");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
} 
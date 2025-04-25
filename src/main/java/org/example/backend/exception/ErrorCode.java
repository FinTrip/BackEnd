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
    USER_BANNED(HttpStatus.FORBIDDEN, "User account has been banned"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Token has expired"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token"),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "Unauthorized access"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "Password does not match"),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "Invalid password format"),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "Invalid email format"),
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "Unauthorized User access"),

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
    //Message
    MESSAGE_NOT_FOUND(HttpStatus.BAD_REQUEST,"Message not found"),
    // General errors
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Database error occurred"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Validation error"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "Bad request"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access forbidden"),

    //Travel
    TRAVELPLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "Travel plan not found"),
    //Post
    ALREADY_LIKED(HttpStatus.BAD_REQUEST, "Already liked"),
    
    // API related errors
    API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "API error occurred"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Service is currently unavailable"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    REQUEST_TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "Request timeout"),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bài viết"),
    TRAVEL_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy kế hoạch du lịch"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),

    //Api Room 
    TOKEN_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Token generation failed"),
    API_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "API call failed"),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Room not found"),
    USER_NOT_AUTHORIZED(HttpStatus.UNAUTHORIZED, "User not authorized"),
    INVALID_ROOM_ID(HttpStatus.BAD_REQUEST, "Invalid room ID"),
    USER_ALREADY_IN_ROOM(HttpStatus.CONFLICT, "User already in room"),
    ROOM_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Room creation failed"),
    ROOM_JOIN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Room join failed"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND,"Comment Not Found"),
    FILE_UPLOAD_ERROR(HttpStatus.NOT_FOUND,"file Not Found"),
    IMAGE_UPLOAD_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload image to Cloudinary"),
    
    // Friendship related errors
    INVALID_OPERATION(HttpStatus.BAD_REQUEST, "Thao tác không hợp lệ"),
    FRIEND_REQUEST_ALREADY_SENT(HttpStatus.CONFLICT, "Lời mời kết bạn đã được gửi"),
    ALREADY_FRIENDS(HttpStatus.CONFLICT, "Hai người dùng đã là bạn bè"),
    USER_BLOCKED(HttpStatus.FORBIDDEN, "Người dùng đã bị chặn"),
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy lời mời kết bạn"),
    NOT_FRIENDS(HttpStatus.BAD_REQUEST, "Hai người dùng không phải là bạn bè"),
    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Chat room not found"),

    // Các lỗi liên quan đến báo cáo
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "Report not found");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
package org.example.backend.exception;

public class PaymentException extends AppException {
    public PaymentException(ErrorCode errorCode) {
        super(errorCode);
    }
    public PaymentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
} 
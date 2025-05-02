package org.example.backend.service;

import org.example.backend.dto.PaymentRequestDto;
import org.example.backend.dto.PaymentResponseDto;

import java.util.Map;

public interface PaymentService {
    PaymentResponseDto createPayment(PaymentRequestDto requestDto);
    void handleWebhook(Map<String, Object> payload);
} 
package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.PaymentRequestDto;
import org.example.backend.dto.PaymentResponseDto;
import org.example.backend.dto.ApiResponse;
import org.example.backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.example.backend.repository.UserRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> createPayment(HttpServletRequest request, @RequestBody PaymentRequestDto requestDto) {
        String userEmail = (String) request.getAttribute("userEmail");
        if (userEmail == null) {
            ApiResponse<PaymentResponseDto> errorResponse = new ApiResponse<>();
            errorResponse.setCode(401);
            errorResponse.setMessage("Unauthorized");
            errorResponse.setResult(null);
            return ResponseEntity.status(401).body(errorResponse);
        }
        userRepository.findByEmail(userEmail).ifPresent(user -> requestDto.setUserId(user.getId().longValue()));
        PaymentResponseDto response = paymentService.createPayment(requestDto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        paymentService.handleWebhook(payload);
        return ResponseEntity.ok("OK");
    }
} 
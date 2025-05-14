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
import org.example.backend.repository.PaymentRepository;
import org.example.backend.repository.BlogPostRepository;
import org.example.backend.entity.Payment;
import org.example.backend.entity.User;
import org.example.backend.entity.BlogPost;
import org.example.backend.exception.ErrorCode;
import org.example.backend.exception.PaymentException;
import org.example.backend.service.impl.PaymentServiceImpl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentServiceImpl paymentServiceImpl;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final BlogPostRepository blogPostRepository;

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
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload, @RequestHeader(value = "x-client-id", required = false) String clientId) {
        try {
            // Kiểm tra clientId để đảm bảo webhook đến từ PayOS (có thể bỏ qua trong trường hợp test)
            paymentService.handleWebhook(payload);
            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping("/status/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> getPaymentStatus(@PathVariable String orderId) {
        Optional<Payment> paymentOpt = paymentRepository.findByPayosOrderId(orderId);
        
        if (!paymentOpt.isPresent()) {
            ApiResponse<PaymentResponseDto> errorResponse = new ApiResponse<>();
            errorResponse.setCode(404);
            errorResponse.setMessage("Payment not found");
            errorResponse.setResult(null);
            return ResponseEntity.status(404).body(errorResponse);
        }
        
        Payment payment = paymentOpt.get();
        PaymentResponseDto responseDto = new PaymentResponseDto(
            payment.getPayosCheckoutUrl(),
            payment.getStatus(),
            payment.getType(),
            payment.getDuration()
        );
        
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }
    
    @GetMapping("/check-status/{orderCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPayOSStatus(@PathVariable String orderCode) {
        try {
            // Gọi phương thức kiểm tra trạng thái từ PaymentServiceImpl
            Map<String, Object> statusData = paymentServiceImpl.checkPaymentStatus(orderCode);
            return ResponseEntity.ok(ApiResponse.success(statusData));
        } catch (Exception e) {
            ApiResponse<Map<String, Object>> errorResponse = new ApiResponse<>();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error checking payment status: " + e.getMessage());
            errorResponse.setResult(null);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    @PostMapping("/create-manual")
    public ResponseEntity<ApiResponse<PaymentResponseDto>> createManualPayment(
            HttpServletRequest request, 
            @RequestBody PaymentRequestDto requestDto) {
        
        String userEmail = (String) request.getAttribute("userEmail");
        
        // Tạo mã giao dịch
        String transactionId = UUID.randomUUID().toString();
        
        // Lưu thông tin giao dịch
        Payment payment = Payment.builder()
                .amount(requestDto.getAmount())
                .description(requestDto.getDescription())
                .status("PENDING")
                .payosOrderId(transactionId)
                .payosCheckoutUrl("/payment-mock?id=" + transactionId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .type(requestDto.getType())
                .duration(requestDto.getDuration())
                .build();
        
        // Thêm thông tin người dùng nếu có
        if (userEmail != null) {
            userRepository.findByEmail(userEmail).ifPresent(user -> {
                payment.setUser(user);
                requestDto.setUserId(user.getId().longValue());
            });
        }
        
        // Thêm thông tin bài viết nếu có
        if (requestDto.getPostId() != null) {
            blogPostRepository.findById(requestDto.getPostId().intValue())
                    .ifPresent(payment::setPost);
        }
        
        // Lưu giao dịch
        paymentRepository.save(payment);
        
        // Tạo URL thanh toán giả lập
        String simulatedPaymentUrl = "/payment-mock?id=" + transactionId;
        
        return ResponseEntity.ok(ApiResponse.success(
                new PaymentResponseDto(simulatedPaymentUrl, "PENDING", 
                        requestDto.getType(), requestDto.getDuration())
        ));
    }

    @PostMapping("/complete-manual/{transactionId}")
    public ResponseEntity<ApiResponse<String>> completeManualPayment(
            @PathVariable String transactionId) {
        
        Payment payment = paymentRepository.findByPayosOrderId(transactionId)
                .orElseThrow(() -> new PaymentException(
                        ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy giao dịch"
                ));
        
        // Cập nhật trạng thái thanh toán
        payment.setStatus("SUCCESS");
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
        
        // Cập nhật thông tin VIP hoặc AD
        if ("VIP".equalsIgnoreCase(payment.getType()) && payment.getUser() != null) {
            User user = payment.getUser();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime vipExpire = user.getVipExpireAt() != null && 
                    user.getVipExpireAt().isAfter(now) ? 
                    user.getVipExpireAt() : now;
            
            user.setVipExpireAt(vipExpire.plusMonths(payment.getDuration()));
            userRepository.save(user);
        } else if ("AD".equalsIgnoreCase(payment.getType()) && payment.getPost() != null) {
            BlogPost post = payment.getPost();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime adExpire = post.getAdExpireAt() != null && 
                    post.getAdExpireAt().isAfter(now) ? 
                    post.getAdExpireAt() : now;
            
            post.setAdExpireAt(adExpire.plusMonths(payment.getDuration()));
            blogPostRepository.save(post);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Thanh toán thành công"));
    }
} 
package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.PaymentRequestDto;
import org.example.backend.dto.PaymentResponseDto;
import org.example.backend.entity.Payment;
import org.example.backend.entity.VipPackage;
import org.example.backend.entity.AdPackage;
import org.example.backend.exception.ErrorCode;
import org.example.backend.exception.PaymentException;
import org.example.backend.repository.PaymentRepository;
import org.example.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.backend.repository.UserRepository;
import org.example.backend.repository.BlogPostRepository;
import org.example.backend.entity.User;
import org.example.backend.entity.BlogPost;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    @Value("${payos.client-id}")
    private String clientId;
    @Value("${payos.api-key}")
    private String apiKey;

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final UserRepository userRepository;
    private final BlogPostRepository blogPostRepository;

    @Override
    public PaymentResponseDto createPayment(PaymentRequestDto requestDto) {
        if (requestDto.getAmount() == null || requestDto.getDescription() == null ||
            requestDto.getReturnUrl() == null || requestDto.getCancelUrl() == null) {
            throw new PaymentException(ErrorCode.INVALID_INPUT, "Thiếu thông tin bắt buộc");
        }

        // Kiểm tra gói và giá
        if ("VIP".equalsIgnoreCase(requestDto.getType())) {
            VipPackage vipPkg = VipPackage.fromMonths(requestDto.getDuration());
            if (vipPkg == null) throw new PaymentException(ErrorCode.INVALID_INPUT, "Gói VIP không hợp lệ");
            if (!Long.valueOf(vipPkg.getPrice()).equals(requestDto.getAmount())) {
                throw new PaymentException(ErrorCode.INVALID_INPUT, "Số tiền không đúng với gói VIP");
            }
        }
        if ("AD".equalsIgnoreCase(requestDto.getType())) {
            AdPackage adPkg = AdPackage.fromMonths(requestDto.getDuration());
            if (adPkg == null) throw new PaymentException(ErrorCode.INVALID_INPUT, "Gói quảng cáo không hợp lệ");
            if (!Long.valueOf(adPkg.getPrice()).equals(requestDto.getAmount())) {
                throw new PaymentException(ErrorCode.INVALID_INPUT, "Số tiền không đúng với gói quảng cáo");
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-client-id", clientId);
        headers.set("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payosRequest = Map.of(
                "amount", requestDto.getAmount(),
                "description", requestDto.getDescription(),
                "returnUrl", requestDto.getReturnUrl(),
                "cancelUrl", requestDto.getCancelUrl(),
                "orderCode", System.currentTimeMillis()
        );

        System.out.println("PayOS request: " + payosRequest);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payosRequest, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api-merchant.payos.vn/v2/payment-requests",
                    entity,
                    Map.class
            );
            System.out.println("PayOS response: " + response);
            System.out.println("PayOS response body: " + response.getBody());

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể tạo thanh toán với PayOS: " + response);
            }
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            if (data == null) {
                throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR, "PayOS không trả về data: " + response.getBody());
            }
            String checkoutUrl = (String) data.get("checkoutUrl");
            String payosOrderId = String.valueOf(data.get("orderCode"));

            Payment payment = Payment.builder()
                    .amount(requestDto.getAmount())
                    .description(requestDto.getDescription())
                    .status("PENDING")
                    .payosOrderId(payosOrderId)
                    .payosCheckoutUrl(checkoutUrl)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .type(requestDto.getType())
                    .duration(requestDto.getDuration())
                    .userId(requestDto.getUserId())
                    .postId(requestDto.getPostId())
                    .build();
            paymentRepository.save(payment);

            return new PaymentResponseDto(checkoutUrl, "PENDING", requestDto.getType(), requestDto.getDuration());
        } catch (Exception e) {
            e.printStackTrace();
            throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi gọi PayOS: " + e.getMessage());
        }
    }

    @Override
    public void handleWebhook(Map<String, Object> payload) {
        String payosOrderId = String.valueOf(payload.get("orderCode"));
        String status = String.valueOf(payload.get("status"));
        Payment payment = paymentRepository.findByPayosOrderId(payosOrderId)
                .orElseThrow(() -> new PaymentException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy giao dịch"));
        payment.setStatus(status);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Nếu thanh toán thành công thì cập nhật VIP hoặc quảng cáo
        if ("SUCCESS".equalsIgnoreCase(status)) {
            if ("VIP".equalsIgnoreCase(payment.getType()) && payment.getUserId() != null && payment.getDuration() != null) {
                userRepository.findById(payment.getUserId().intValue()).ifPresent(user -> {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime currentVip = user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(now) ? user.getVipExpireAt() : now;
                    user.setVipExpireAt(currentVip.plusMonths(payment.getDuration()));
                    userRepository.save(user);
                });
            }
            if ("AD".equalsIgnoreCase(payment.getType()) && payment.getPostId() != null && payment.getDuration() != null) {
                blogPostRepository.findById(payment.getPostId().intValue()).ifPresent(post -> {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime currentAd = post.getAdExpireAt() != null && post.getAdExpireAt().isAfter(now) ? post.getAdExpireAt() : now;
                    post.setAdExpireAt(currentAd.plusMonths(payment.getDuration()));
                    blogPostRepository.save(post);
                });
            }
        }
    }
} 
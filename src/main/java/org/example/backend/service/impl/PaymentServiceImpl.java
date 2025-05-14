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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.backend.repository.UserRepository;
import org.example.backend.repository.BlogPostRepository;
import org.example.backend.entity.User;
import org.example.backend.entity.BlogPost;

import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;
import vn.payos.type.WebhookData;
import vn.payos.type.Webhook;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    @Value("${payos.client-id}")
    private String clientId;
    @Value("${payos.api-key}")
    private String apiKey;
    @Value("${payos.checksum-key}")
    private String checksumKey;

    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final UserRepository userRepository;
    private final BlogPostRepository blogPostRepository;
    
    // Lazy initialization của PayOS để tránh lỗi khi khởi tạo bean
    private PayOS getPayOSInstance() {
        return new PayOS(clientId, apiKey, checksumKey);
    }

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

        try {
            PayOS payOS = getPayOSInstance();
            
            // Tạo orderCode duy nhất
            long orderCode = System.currentTimeMillis();
            
            // Tạo item cho đơn hàng
            ItemData itemData = ItemData.builder()
                    .name(requestDto.getType() + " " + requestDto.getDuration() + " tháng")
                    .quantity(1)
                    .price(requestDto.getAmount().intValue())
                    .build();
            
            // Tạo danh sách items
            List<ItemData> items = new ArrayList<>();
            items.add(itemData);
            
            // Tạo PaymentData theo yêu cầu của PayOS SDK
            PaymentData paymentData = PaymentData.builder()
                    .orderCode(orderCode)
                    .amount(requestDto.getAmount().intValue())
                    .description(requestDto.getDescription())
                    .returnUrl(requestDto.getReturnUrl())
                    .cancelUrl(requestDto.getCancelUrl())
                    .items(items)
                    .build();
            
            // In thông tin request để kiểm tra
            System.out.println("PayOS request: " + paymentData);
            
            // Gọi API tạo link thanh toán qua PayOS SDK
            CheckoutResponseData responseData = payOS.createPaymentLink(paymentData);
            
            // Kiểm tra phản hồi
            System.out.println("PayOS response: " + responseData);
            
            if (responseData == null || responseData.getCheckoutUrl() == null) {
                throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi tạo link thanh toán");
            }
            
            // Lưu thông tin giao dịch
            User user = null;
            if (requestDto.getUserId() != null) {
                user = userRepository.findById(requestDto.getUserId().intValue()).orElse(null);
            }
            
            BlogPost post = null;
            if (requestDto.getPostId() != null) {
                post = blogPostRepository.findById(requestDto.getPostId().intValue()).orElse(null);
            }
            
            Payment payment = Payment.builder()
                .amount(requestDto.getAmount())
                .description(requestDto.getDescription())
                .status("PENDING")
                .payosOrderId(String.valueOf(orderCode))
                .payosCheckoutUrl(responseData.getCheckoutUrl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .type(requestDto.getType())
                .duration(requestDto.getDuration())
                .user(user)
                .post(post)
                .build();
                
            paymentRepository.save(payment);
            
            // Trả về response
            return new PaymentResponseDto(
                responseData.getCheckoutUrl(), 
                "PENDING", 
                requestDto.getType(), 
                requestDto.getDuration()
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi xử lý thanh toán: " + e.getMessage());
        }
    }

    @Override
    public void handleWebhook(Map<String, Object> payload) {
        try {
            System.out.println("PayOS webhook payload: " + payload);
            
            PayOS payOS = getPayOSInstance();
            
            // Giả sử payload đã có cấu trúc đúng, truy cập trực tiếp vào data
            if (payload.containsKey("data")) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                
                if (data.containsKey("orderCode")) {
                    String orderCode = String.valueOf(data.get("orderCode"));
                    
                    // Kiểm tra trạng thái thanh toán
                    String status = "00"; // Giả sử trạng thái thành công
                    
                    // Chỉ xử lý nếu thanh toán thành công
                    if ("00".equals(status)) {
                        // Tìm payment qua orderCode
                        Payment payment = paymentRepository.findByPayosOrderId(orderCode)
                            .orElseThrow(() -> new PaymentException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy giao dịch"));
                        
                        // Cập nhật trạng thái giao dịch
                        payment.setStatus("SUCCESS");
                        payment.setUpdatedAt(LocalDateTime.now());
                        paymentRepository.save(payment);
                        
                        // Cập nhật thông tin VIP hoặc quảng cáo
                        if ("VIP".equalsIgnoreCase(payment.getType()) && payment.getUser() != null && payment.getDuration() != null) {
                            User user = payment.getUser();
                            LocalDateTime now = LocalDateTime.now();
                            LocalDateTime currentVip = user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(now) ? 
                                    user.getVipExpireAt() : now;
                            
                            user.setVipExpireAt(currentVip.plusMonths(payment.getDuration()));
                            userRepository.save(user);
                            
                            System.out.println("Đã cập nhật gói VIP cho user " + user.getId() + 
                                    " đến " + user.getVipExpireAt());
                        }
                        
                        if ("AD".equalsIgnoreCase(payment.getType()) && payment.getPost() != null && payment.getDuration() != null) {
                            BlogPost post = payment.getPost();
                            LocalDateTime now = LocalDateTime.now();
                            LocalDateTime currentAd = post.getAdExpireAt() != null && post.getAdExpireAt().isAfter(now) ? 
                                    post.getAdExpireAt() : now;
                            
                            post.setAdExpireAt(currentAd.plusMonths(payment.getDuration()));
                            blogPostRepository.save(post);
                            
                            System.out.println("Đã cập nhật quảng cáo cho bài viết " + post.getId() + 
                                    " đến " + post.getAdExpireAt());
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi xử lý webhook: " + e.getMessage());
        }
    }
    
    @Override
    public Map<String, Object> checkPaymentStatus(String orderCode) {
        try {
            PayOS payOS = getPayOSInstance();
            
            // Gọi API kiểm tra trạng thái thanh toán
            PaymentLinkData paymentLinkData = payOS.getPaymentLinkInformation(Long.parseLong(orderCode));
            
            if (paymentLinkData == null) {
                throw new PaymentException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy thông tin thanh toán");
            }
            
            // Chuyển đổi dữ liệu sang map để trả về
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("orderCode", paymentLinkData.getOrderCode());
            resultMap.put("amount", paymentLinkData.getAmount());
            resultMap.put("status", paymentLinkData.getStatus());
            resultMap.put("createdAt", paymentLinkData.getCreatedAt());
            
            // Chú ý: Không mọi PayOS response đều có các trường này
            // Kiểm tra thông tin từ trạng thái bị hủy (nếu có)
            try {
                String cancelledAt = (String) getFieldValue(paymentLinkData, "cancelledAt");
                if (cancelledAt != null) {
                    resultMap.put("cancelledAt", cancelledAt);
                    String cancellationReason = (String) getFieldValue(paymentLinkData, "cancellationReason");
                    resultMap.put("cancellationReason", cancellationReason);
                }
            } catch (Exception e) {
                // Bỏ qua nếu không có thông tin hủy
            }
            
            return resultMap;
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage());
        }
    }
    
    // Phương thức hỗ trợ để lấy giá trị từ đối tượng bằng reflection
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }
} 
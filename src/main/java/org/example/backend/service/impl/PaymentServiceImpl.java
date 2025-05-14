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
            
            // Kiểm tra và log toàn bộ cấu trúc payload để debug
            if (payload == null) {
                System.out.println("Webhook payload is null");
                return;
            }
            
            // Log tất cả các key trong payload để xem cấu trúc
            System.out.println("Webhook payload keys: " + payload.keySet());
            
            // Trong một số trường hợp, data có thể nằm trong trường hợp khác
            Object dataObj = null;
            if (payload.containsKey("data")) {
                dataObj = payload.get("data");
            } else if (payload.containsKey("result")) {
                dataObj = payload.get("result");
            } else if (payload.containsKey("resource")) {
                dataObj = payload.get("resource");
            }
            
            if (dataObj == null) {
                System.out.println("Cannot find data in webhook payload");
                // In toàn bộ payload để debug
                for (Map.Entry<String, Object> entry : payload.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
                return;
            }
            
            // Chuyển đổi data object thành Map
            Map<String, Object> data;
            if (dataObj instanceof Map) {
                data = (Map<String, Object>) dataObj;
            } else {
                System.out.println("Data is not a Map: " + dataObj.getClass().getName());
                return;
            }
            
            // Log tất cả các key trong data để xem cấu trúc
            System.out.println("Data keys: " + data.keySet());
            
            // Tìm orderCode trong các trường phổ biến
            final String finalOrderCode;
            String tempOrderCode = null;
            if (data.containsKey("orderCode")) {
                tempOrderCode = String.valueOf(data.get("orderCode"));
            } else if (data.containsKey("orderId")) {
                tempOrderCode = String.valueOf(data.get("orderId"));
            } else if (data.containsKey("order_code")) {
                tempOrderCode = String.valueOf(data.get("order_code"));
            }
            finalOrderCode = tempOrderCode;
            
            // Tìm status trong các trường phổ biến
            String status = null;
            // Đầu tiên, kiểm tra PayOS code=00 đặc biệt (thành công)
            boolean isPayOSSuccess = false;
            if (data.containsKey("code") && "00".equals(String.valueOf(data.get("code")))) {
                isPayOSSuccess = true;
                status = "PAID"; // Đặt status mặc định là PAID nếu code=00
            } else if (data.containsKey("status")) {
                status = String.valueOf(data.get("status"));
            } else if (data.containsKey("paymentStatus")) {
                status = String.valueOf(data.get("paymentStatus"));
            } else if (data.containsKey("payment_status")) {
                status = String.valueOf(data.get("payment_status"));
            } else if (data.containsKey("state")) {
                status = String.valueOf(data.get("state"));
            }
            
            if (finalOrderCode == null) {
                System.out.println("Missing orderCode in webhook data");
                // In toàn bộ data để debug
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
                return;
            }
            
            System.out.println("Processing webhook for orderCode: " + finalOrderCode + " with status: " + status + 
                ", PayOS success: " + isPayOSSuccess);
            
            // Chỉ xử lý nếu thanh toán thành công (status có thể là PAID, SUCCESS, COMPLETED, v.v. hoặc isPayOSSuccess=true)
            if (isPayOSSuccess || isSuccessStatus(status)) {
                // Tìm payment qua orderCode
                Payment payment = paymentRepository.findByPayosOrderId(finalOrderCode)
                    .orElseThrow(() -> new PaymentException(ErrorCode.RESOURCE_NOT_FOUND, 
                            "Không tìm thấy giao dịch với orderCode: " + finalOrderCode));
                
                // Cập nhật trạng thái giao dịch
                payment.setStatus("SUCCESS");
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                
                System.out.println("Đã cập nhật trạng thái thanh toán thành SUCCESS cho đơn hàng: " + finalOrderCode);
                
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
            } else {
                System.out.println("Trạng thái thanh toán không phải thành công: " + status + 
                    ", PayOS success: " + isPayOSSuccess);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi xử lý webhook: " + e.getMessage());
        }
    }
    
    // Phương thức helper để kiểm tra nếu status là thành công
    private boolean isSuccessStatus(String status) {
        if (status == null) return false;
        
        status = status.toUpperCase();
        return status.equals("PAID") || 
               status.equals("SUCCESS") || 
               status.equals("COMPLETED") || 
               status.equals("SUCCESSFUL") ||
               status.equals("00"); // Một số cổng thanh toán dùng mã "00" cho thành công
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
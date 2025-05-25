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
import org.example.backend.service.WalletService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.example.backend.repository.UserRepository;
import org.example.backend.repository.BlogPostRepository;
import org.example.backend.entity.User;
import org.example.backend.entity.BlogPost;
import org.example.backend.entity.WalletTransaction;

import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;
import vn.payos.type.PaymentLinkData;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final BlogPostRepository blogPostRepository;
    private final WalletService walletService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Value("${payos.client-id}")
    private String clientId;
    
    @Value("${payos.api-key}")
    private String apiKey;
    
    @Value("${payos.checksum-key}")
    private String checksumKey;
    
    private PayOS payOS;
    
    private PayOS getPayOS() {
        if (payOS == null) {
            payOS = new PayOS(clientId, apiKey, checksumKey);
        }
        return payOS;
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
        // Kiểm tra nếu là nạp tiền vào ví
        if ("WALLET".equalsIgnoreCase(requestDto.getType())) {
            if (requestDto.getAmount() <= 0) {
                throw new PaymentException(ErrorCode.INVALID_INPUT, "Số tiền nạp phải lớn hơn 0");
            }
        }

        try {
            // Tạo tham số cho PayOS
            List<ItemData> items = new ArrayList<>();
            items.add(ItemData.builder()
                .name(requestDto.getDescription())
                .price(requestDto.getAmount().intValue())
                .quantity(1)
                .build());
            
            long orderCode = System.currentTimeMillis();
            
            PaymentData paymentData = PaymentData.builder()
                .orderCode(orderCode)
                .amount(requestDto.getAmount().intValue())
                .description(requestDto.getDescription())
                .items(items)
                .cancelUrl(requestDto.getCancelUrl())
                .returnUrl(requestDto.getReturnUrl())
                .build();
            
            // Gọi PayOS API để tạo thanh toán
            CheckoutResponseData responseData = getPayOS().createPaymentLink(paymentData);
            
            if (responseData == null || responseData.getCheckoutUrl() == null) {
                throw new PaymentException(ErrorCode.PAYMENT_FAILED, "Không thể tạo liên kết thanh toán");
            }
            
            // Lưu thông tin thanh toán vào cơ sở dữ liệu
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
    public Map<String, Object> checkPaymentStatus(String orderCode) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // Kiểm tra trong database trước
            paymentRepository.findByPayosOrderId(orderCode).ifPresent(payment -> {
                result.put("status", payment.getStatus());
                result.put("amount", payment.getAmount());
                result.put("description", payment.getDescription());
                result.put("payosOrderId", payment.getPayosOrderId());
                result.put("createdAt", payment.getCreatedAt().toString());
                result.put("updatedAt", payment.getUpdatedAt().toString());
            });
            
            if (!result.isEmpty() && "SUCCESS".equals(result.get("status"))) {
                return result;
            }
            
            // Gọi PayOS API để kiểm tra trạng thái
            try {
                // Kiểm tra trạng thái từ PayOS API
                PaymentLinkData paymentInfo = getPayOS().getPaymentLinkInformation(Long.parseLong(orderCode));
                if (paymentInfo != null && paymentInfo.getStatus() != null) {
                    result.put("payosStatus", paymentInfo.getStatus());
                    
                    // Cập nhật trạng thái trong database nếu thanh toán thành công từ PayOS
                    if ("PAID".equals(paymentInfo.getStatus())) {
                        paymentRepository.findByPayosOrderId(orderCode).ifPresent(payment -> {
                            payment.setStatus("SUCCESS");
                            payment.setUpdatedAt(LocalDateTime.now());
                            paymentRepository.save(payment);
                            
                            // Cập nhật thông tin VIP hoặc quảng cáo
                            processSuccessfulPayment(payment);
                            
                            result.put("status", "SUCCESS");
                        });
                    }
                }
            } catch (Exception e) {
                System.out.println("Lỗi khi kiểm tra với PayOS API: " + e.getMessage());
            }
            
            return result;
        } catch (Exception e) {
            throw new PaymentException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi kiểm tra trạng thái thanh toán: " + e.getMessage());
        }
    }

    @Override
    public void handleWebhook(Map<String, Object> payload) {
        try {
            if (payload == null) {
                throw new PaymentException(ErrorCode.INVALID_INPUT, "Payload rỗng");
            }
            
            System.out.println("Received webhook: " + payload);
            
            // Sử dụng mảng để lưu giá trị mà vẫn giữ tham chiếu final
            final String[] orderCodeRef = new String[1];
            final String[] statusRef = new String[1];
            final boolean[] isSuccessRef = new boolean[1];
            
            // Kiểm tra format của callback từ PayOS
            if (payload.get("orderCode") != null) {
                // Format webhook từ PayOS trực tiếp
                orderCodeRef[0] = payload.get("orderCode").toString();
                Object statusObj = payload.get("status");
                statusRef[0] = statusObj != null ? statusObj.toString() : null;
            } else if (payload.get("data") != null && payload.get("data") instanceof Map) {
                // Format webhook khác (có thể là từ middleware hoặc proxy)
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                
                if (data.get("orderCode") != null) {
                    orderCodeRef[0] = data.get("orderCode").toString();
                }
                
                if (data.get("status") != null) {
                    statusRef[0] = data.get("status").toString();
                }
            }
            
            // Đối với PayOS, nếu code=00 hoặc status=SUCCESS thì coi là thành công
            if (payload.get("code") != null && "00".equals(payload.get("code").toString())) {
                isSuccessRef[0] = true;
            }
            
            System.out.println("Processing webhook for orderCode: " + orderCodeRef[0] + " with status: " + statusRef[0] + 
                ", PayOS success: " + isSuccessRef[0]);
            
            // Chỉ xử lý nếu thanh toán thành công (status có thể là PAID, SUCCESS, COMPLETED)
            if (isSuccessRef[0] || isSuccessStatus(statusRef[0])) {
                // Tìm payment qua orderCode
                Payment payment = paymentRepository.findByPayosOrderId(orderCodeRef[0])
                    .orElseThrow(() -> new PaymentException(ErrorCode.RESOURCE_NOT_FOUND, 
                            "Không tìm thấy giao dịch với orderCode: " + orderCodeRef[0]));
                
                // Cập nhật trạng thái giao dịch
                payment.setStatus("SUCCESS");
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
                
                System.out.println("Đã cập nhật trạng thái thanh toán thành SUCCESS cho đơn hàng: " + orderCodeRef[0]);
                
                // Xử lý thanh toán thành công
                processSuccessfulPayment(payment);
            } else {
                System.out.println("Trạng thái thanh toán không phải thành công: " + statusRef[0] + 
                    ", PayOS success: " + isSuccessRef[0]);
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
    
    // Phương thức xử lý các thanh toán thành công
    private void processSuccessfulPayment(Payment payment) {
        if (payment.getUser() == null) {
            System.out.println("Không tìm thấy thông tin người dùng cho thanh toán: " + payment.getId());
            return;
        }
        
        User user = payment.getUser();
        
        // Đảm bảo walletBalance không null
        if (user.getWalletBalance() == null) {
            user.setWalletBalance(0L);
            userRepository.save(user);
        }
        
        // Xử lý theo loại thanh toán
        if ("WALLET".equalsIgnoreCase(payment.getType())) {
            // Nạp tiền vào ví
            System.out.println("Bắt đầu nạp tiền vào ví cho user: " + user.getId() + 
                ", số dư hiện tại: " + user.getWalletBalance() + 
                ", số tiền nạp: " + payment.getAmount());
                
            WalletTransaction transaction = walletService.depositFunds(user, payment.getAmount(), payment);
            
            // Kiểm tra lại số dư sau khi nạp
            User updatedUser = userRepository.findById(user.getId()).orElse(null);
            if (updatedUser != null) {
                System.out.println("Sau khi nạp tiền: User ID " + updatedUser.getId() + 
                    ", số dư: " + updatedUser.getWalletBalance() +
                    ", giao dịch ID: " + transaction.getId());
            } else {
                System.out.println("Không thể tìm thấy user sau khi nạp tiền");
            }
        } 
        else if ("VIP".equalsIgnoreCase(payment.getType()) && payment.getDuration() != null) {
            // Kiểm tra số dư ví
            if (user.getWalletBalance() < payment.getAmount()) {
                System.out.println("Số dư không đủ để mua gói VIP. Cần: " + payment.getAmount() + 
                    ", Hiện có: " + user.getWalletBalance());
                return;
            }
            
            // Trừ tiền từ ví
            walletService.deductFunds(
                user, 
                payment.getAmount(), 
                WalletTransaction.TransactionType.PURCHASE_VIP, 
                "Mua gói VIP " + payment.getDuration() + " tháng"
            );
            
            // Cập nhật thời hạn VIP
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime currentVip = user.getVipExpireAt() != null && user.getVipExpireAt().isAfter(now) ? 
                    user.getVipExpireAt() : now;
            
            user.setVipExpireAt(currentVip.plusMonths(payment.getDuration()));
            userRepository.save(user);
            
            System.out.println("Đã cập nhật gói VIP cho user " + user.getId() + 
                    " đến " + user.getVipExpireAt());
        } 
        else if ("AD".equalsIgnoreCase(payment.getType()) && payment.getPost() != null && payment.getDuration() != null) {
            // Kiểm tra số dư ví
            if (user.getWalletBalance() < payment.getAmount()) {
                System.out.println("Số dư không đủ để mua gói quảng cáo. Cần: " + payment.getAmount() + 
                    ", Hiện có: " + user.getWalletBalance());
                return;
            }
            
            // Trừ tiền từ ví
            walletService.deductFunds(
                user, 
                payment.getAmount(), 
                WalletTransaction.TransactionType.PURCHASE_AD, 
                "Mua quảng cáo " + payment.getDuration() + " tháng cho bài viết ID: " + payment.getPost().getId()
            );
            
            // Cập nhật thời hạn quảng cáo
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
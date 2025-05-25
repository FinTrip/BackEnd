package org.example.backend.controller;

import lombok.RequiredArgsConstructor;
import org.example.backend.dto.ApiResponse;
import org.example.backend.dto.WalletTransactionDto;
import org.example.backend.entity.User;
import org.example.backend.entity.WalletTransaction;
import org.example.backend.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.example.backend.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    /**
     * Lấy thông tin người dùng hiện tại từ token JWT
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new RuntimeException("Không có thông tin xác thực");
        }
        
        String email = authentication.getName();
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Không có thông tin email người dùng");
        }
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng với email: " + email));
    }

    /**
     * Lấy số dư ví của người dùng hiện tại
     */
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Long>> getWalletBalance() {
        try {
            User currentUser = getCurrentUser();
            System.out.println("Đang lấy số dư ví cho user: " + currentUser.getId() + ", email: " + currentUser.getEmail());
            
            Long balance = walletService.getWalletBalance(currentUser.getId());
            
            // Đảm bảo không trả về null
            balance = balance != null ? balance : 0L;
            
            System.out.println("Số dư ví của user " + currentUser.getId() + ": " + balance);
            
            return ResponseEntity.ok(ApiResponse.success(balance));
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy số dư ví: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi khi lấy số dư ví: " + e.getMessage())
            );
        }
    }

    /**
     * Lấy lịch sử giao dịch của người dùng hiện tại
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<WalletTransactionDto>>> getTransactionHistory() {
        User currentUser = getCurrentUser();
        List<WalletTransaction> transactions = walletService.getTransactionHistory(currentUser.getId());
        List<WalletTransactionDto> transactionDtos = transactions.stream()
                .map(WalletTransactionDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(transactionDtos));
    }
    
    /**
     * Tạo thanh toán để nạp tiền vào ví
     */
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createWalletDeposit(
            @RequestBody Map<String, Object> request) {
        
        User currentUser = getCurrentUser();
        Long amount = Long.valueOf(request.get("amount").toString());
        
        Map<String, Object> result = Map.of(
            "userId", currentUser.getId(),
            "amount", amount,
            "redirectUrl", "/api/payment/create-wallet-payment?amount=" + amount + "&userId=" + currentUser.getId()
        );
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    /**
     * Duy trì các API cũ để tương thích ngược (có thể bỏ sau này)
     */
    @GetMapping("/balance/{userId}")
    public ResponseEntity<ApiResponse<Long>> getWalletBalanceById(@PathVariable Integer userId) {
        try {
            // Kiểm tra quyền - chỉ cho phép xem số dư của chính mình hoặc ADMIN có thể xem của người khác
            User currentUser = getCurrentUser();
            if (!currentUser.getId().equals(userId)) {
                // Kiểm tra xem người dùng hiện tại có phải ADMIN không
                boolean isAdmin = currentUser.getRole() != null && 
                        "ADMIN".equalsIgnoreCase(currentUser.getRole().getRoleName());
                if (!isAdmin) {
                    return ResponseEntity.status(403).body(
                        ApiResponse.error("Không có quyền xem thông tin ví của người khác")
                    );
                }
            }
            
            Long balance = walletService.getWalletBalance(userId);
            // Đảm bảo không trả về null
            balance = balance != null ? balance : 0L;
            
            return ResponseEntity.ok(ApiResponse.success(balance));
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy số dư ví theo ID: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                ApiResponse.error("Lỗi khi lấy số dư ví: " + e.getMessage())
            );
        }
    }
    
    @GetMapping("/transactions/{userId}")
    public ResponseEntity<ApiResponse<List<WalletTransactionDto>>> getTransactionHistoryById(@PathVariable Integer userId) {
        // Kiểm tra quyền - tương tự như trên
        User currentUser = getCurrentUser();
        if (!currentUser.getId().equals(userId)) {
            boolean isAdmin = currentUser.getRole() != null && 
                    "ADMIN".equalsIgnoreCase(currentUser.getRole().getRoleName());
            if (!isAdmin) {
                return ResponseEntity.status(403).body(
                    ApiResponse.error("Không có quyền xem lịch sử giao dịch của người khác")
                );
            }
        }
        
        List<WalletTransaction> transactions = walletService.getTransactionHistory(userId);
        List<WalletTransactionDto> transactionDtos = transactions.stream()
                .map(WalletTransactionDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(transactionDtos));
    }
} 
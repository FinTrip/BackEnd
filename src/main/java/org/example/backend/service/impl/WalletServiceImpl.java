package org.example.backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.backend.entity.User;
import org.example.backend.entity.WalletTransaction;
import org.example.backend.entity.Payment;
import org.example.backend.exception.ErrorCode;
import org.example.backend.exception.PaymentException;
import org.example.backend.repository.UserRepository;
import org.example.backend.repository.WalletTransactionRepository;
import org.example.backend.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public WalletTransaction depositFunds(User user, Long amount, Payment payment) {
        if (amount <= 0) {
            throw new PaymentException(ErrorCode.INVALID_INPUT, "Số tiền phải lớn hơn 0");
        }

        // Đảm bảo wallet balance không null
        if (user.getWalletBalance() == null) {
            user.setWalletBalance(0L);
        }

        // Cập nhật số dư ví
        user.setWalletBalance(user.getWalletBalance() + amount);
        User savedUser = userRepository.save(user);
        
        System.out.println("Đã cập nhật ví của user " + user.getId() + " từ " + 
            (user.getWalletBalance() - amount) + " lên " + user.getWalletBalance());

        // Tạo giao dịch
        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .amount(amount)
                .type(WalletTransaction.TransactionType.DEPOSIT)
                .description("Nạp tiền vào ví")
                .payment(payment)
                .build();

        return walletTransactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public WalletTransaction deductFunds(User user, Long amount, WalletTransaction.TransactionType type, String description) {
        if (amount <= 0) {
            throw new PaymentException(ErrorCode.INVALID_INPUT, "Số tiền phải lớn hơn 0");
        }

        // Đảm bảo wallet balance không null
        if (user.getWalletBalance() == null) {
            user.setWalletBalance(0L);
        }

        if (user.getWalletBalance() < amount) {
            throw new PaymentException(ErrorCode.INSUFFICIENT_FUNDS, "Số dư không đủ để thực hiện giao dịch");
        }

        // Cập nhật số dư ví
        user.setWalletBalance(user.getWalletBalance() - amount);
        User savedUser = userRepository.save(user);
        
        System.out.println("Đã trừ " + amount + " từ ví của user " + user.getId() + 
            ", số dư còn lại: " + user.getWalletBalance());

        // Tạo giao dịch
        WalletTransaction transaction = WalletTransaction.builder()
                .user(user)
                .amount(-amount) // Số tiền âm để biểu thị việc rút tiền
                .type(type)
                .description(description)
                .build();

        return walletTransactionRepository.save(transaction);
    }

    @Override
    public Long getWalletBalance(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PaymentException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng"));
        
        // Đảm bảo không trả về null
        if (user.getWalletBalance() == null) {
            user.setWalletBalance(0L);
            user = userRepository.save(user);
        }
        
        return user.getWalletBalance();
    }

    @Override
    public List<WalletTransaction> getTransactionHistory(Integer userId) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
} 
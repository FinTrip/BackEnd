package org.example.backend.repository;

import org.example.backend.entity.WalletTransaction;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserOrderByCreatedAtDesc(User user);
    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(Integer userId);
} 
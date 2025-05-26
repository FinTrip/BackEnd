package org.example.backend.repository;

import org.example.backend.entity.Payment;
import org.example.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPayosOrderId(String payosOrderId);
    List<Payment> findByUserOrderByCreatedAtDesc(User user);
    List<Payment> findByUserIdOrderByCreatedAtDesc(Integer userId);
    List<Payment> findAllByOrderByCreatedAtDesc();
} 
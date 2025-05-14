package org.example.backend.service;

import org.example.backend.entity.User;
import org.example.backend.entity.WalletTransaction;
import org.example.backend.entity.Payment;

import java.util.List;

public interface WalletService {
    /**
     * Adds funds to a user's wallet
     * @param user the user
     * @param amount the amount to add
     * @param payment the related payment, if applicable
     * @return the created transaction
     */
    WalletTransaction depositFunds(User user, Long amount, Payment payment);
    
    /**
     * Deducts funds from a user's wallet
     * @param user the user
     * @param amount the amount to deduct
     * @param type the transaction type
     * @param description transaction description
     * @return the created transaction
     */
    WalletTransaction deductFunds(User user, Long amount, WalletTransaction.TransactionType type, String description);
    
    /**
     * Get user's current wallet balance
     * @param userId the user ID
     * @return the wallet balance
     */
    Long getWalletBalance(Integer userId);
    
    /**
     * Get transaction history for a user
     * @param userId the user ID
     * @return list of transactions
     */
    List<WalletTransaction> getTransactionHistory(Integer userId);
} 
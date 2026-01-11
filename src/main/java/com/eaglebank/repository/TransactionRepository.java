package com.eaglebank.repository;

import com.eaglebank.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    List<Transaction> findByAccount_AccountNumberOrderByCreatedAtDesc(String accountNumber);

    Optional<Transaction> findByTransactionIdAndAccount_AccountNumber(String transactionId, String accountNumber);

    boolean existsByTransactionId(String transactionId);
}


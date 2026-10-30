package com.bank.transaction.service;

import com.bank.transaction.config.TransactionProperties;
import com.bank.transaction.domain.Transaction;
import com.bank.transaction.exception.InvalidTransactionException;
import com.bank.transaction.exception.TransactionNotFoundException;
import com.bank.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionProperties properties;

    public TransactionService(TransactionRepository transactionRepository, TransactionProperties properties) {
        this.transactionRepository = transactionRepository;
        this.properties = properties;
    }

    public Transaction create(CreateTransactionCommand command) {
        validateAmount(command.amount());
        Transaction transaction = new Transaction();
        transaction.setAccountId(command.accountId());
        transaction.setAmount(command.amount());
        transaction.setCurrency(command.currency().toUpperCase(Locale.ROOT));
        transaction.setMerchantCategory(command.merchantCategory());
        transaction.setCountry(command.country());
        transaction.setCreatedAt(Instant.now());
        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public Transaction get(UUID id) {
        return transactionRepository.findById(id).orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Transaction> list() {
        return transactionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Transaction> listByAccount(UUID accountId) {
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidTransactionException("Amount must be strictly positive");
        }
        BigDecimal maxAmount = properties.maxAmount();
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            throw new InvalidTransactionException("Amount exceeds the configured maximum of " + maxAmount.toPlainString());
        }
    }
}

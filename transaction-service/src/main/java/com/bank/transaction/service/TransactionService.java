package com.bank.transaction.service;

import com.bank.transaction.client.AccountClient;
import com.bank.transaction.client.AccountDto;
import com.bank.transaction.client.AmountRequest;
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

    static final String TRANSFER_OUT = "TRANSFER_OUT";
    static final String TRANSFER_IN = "TRANSFER_IN";
    static final String INTERNAL_COUNTRY = "INTERNAL";

    private final TransactionRepository transactionRepository;
    private final TransactionProperties properties;
    private final AccountClient accountClient;

    public TransactionService(TransactionRepository transactionRepository,
                              TransactionProperties properties,
                              AccountClient accountClient) {
        this.transactionRepository = transactionRepository;
        this.properties = properties;
        this.accountClient = accountClient;
    }

    public Transaction create(CreateTransactionCommand command) {
        validateAmount(command.amount());
        String currency = command.currency().toUpperCase(Locale.ROOT);
        AccountDto account = accountClient.getAccount(command.accountId());
        requireUsable(account, currency);
        requireSufficientBalance(account, command.amount());
        Transaction transaction = save(command.accountId(), command.amount(), currency,
                command.merchantCategory(), command.country());
        accountClient.debit(command.accountId(), new AmountRequest(command.amount().doubleValue()));
        return transaction;
    }

    public TransferResult transfer(TransferCommand command) {
        validateAmount(command.amount());
        if (command.sourceAccountId().equals(command.targetAccountId())) {
            throw new InvalidTransactionException("Source and target accounts must be different");
        }
        String currency = command.currency().toUpperCase(Locale.ROOT);
        AccountDto source = accountClient.getAccount(command.sourceAccountId());
        AccountDto target = accountClient.getAccount(command.targetAccountId());
        requireUsable(source, currency);
        requireUsable(target, currency);
        requireSufficientBalance(source, command.amount());

        Transaction debit = save(command.sourceAccountId(), command.amount(), currency, TRANSFER_OUT, INTERNAL_COUNTRY);
        Transaction credit = save(command.targetAccountId(), command.amount(), currency, TRANSFER_IN, INTERNAL_COUNTRY);

        AmountRequest amountRequest = new AmountRequest(command.amount().doubleValue());
        accountClient.debit(command.sourceAccountId(), amountRequest);
        try {
            accountClient.credit(command.targetAccountId(), amountRequest);
        } catch (RuntimeException failure) {
            restoreSource(command.sourceAccountId(), amountRequest, failure);
            throw failure;
        }
        return new TransferResult(debit, credit);
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

    private Transaction save(UUID accountId, BigDecimal amount, String currency, String merchantCategory, String country) {
        Transaction transaction = new Transaction();
        transaction.setAccountId(accountId);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setMerchantCategory(merchantCategory);
        transaction.setCountry(country);
        transaction.setCreatedAt(Instant.now());
        return transactionRepository.save(transaction);
    }

    private void restoreSource(UUID sourceAccountId, AmountRequest amountRequest, RuntimeException failure) {
        try {
            accountClient.credit(sourceAccountId, amountRequest);
        } catch (RuntimeException restoreFailure) {
            failure.addSuppressed(restoreFailure);
        }
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

    private void requireUsable(AccountDto account, String currency) {
        if (!account.isActive()) {
            throw new InvalidTransactionException("Account " + account.id() + " is not active");
        }
        if (!account.currency().equalsIgnoreCase(currency)) {
            throw new InvalidTransactionException("Transaction currency " + currency
                    + " does not match account currency " + account.currency());
        }
    }

    private void requireSufficientBalance(AccountDto account, BigDecimal amount) {
        if (BigDecimal.valueOf(account.balance()).compareTo(amount) < 0) {
            throw new InvalidTransactionException("Insufficient balance on account " + account.id());
        }
    }
}

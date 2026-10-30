package com.bank.account.service;

import com.bank.account.domain.Account;
import com.bank.account.domain.AccountStatus;
import com.bank.account.exception.AccountNotFoundException;
import com.bank.account.exception.InvalidOperationException;
import com.bank.account.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public Account create(String customerReference, String currency, double initialBalance) {
        if (initialBalance < 0) {
            throw new InvalidOperationException("Initial balance must not be negative");
        }
        Account account = new Account();
        account.setCustomerReference(customerReference);
        account.setCurrency(currency.toUpperCase(Locale.ROOT));
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(initialBalance);
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public Account get(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Account> list() {
        return accountRepository.findAll();
    }

    public Account updateStatus(UUID id, AccountStatus status) {
        Account account = get(id);
        account.setStatus(status);
        return account;
    }

    public Account updateBalance(UUID id, double balance) {
        if (balance < 0) {
            throw new InvalidOperationException("Balance must not be negative");
        }
        Account account = get(id);
        account.setBalance(balance);
        return account;
    }

    public Account credit(UUID id, double amount) {
        requirePositive(amount);
        Account account = requireActive(id);
        account.setBalance(account.getBalance() + amount);
        return account;
    }

    public Account debit(UUID id, double amount) {
        requirePositive(amount);
        Account account = requireActive(id);
        if (account.getBalance() < amount) {
            throw new InvalidOperationException("Insufficient balance on account " + id);
        }
        account.setBalance(account.getBalance() - amount);
        return account;
    }

    private Account requireActive(UUID id) {
        Account account = get(id);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new InvalidOperationException("Account " + id + " is not active");
        }
        return account;
    }

    private void requirePositive(double amount) {
        if (amount <= 0) {
            throw new InvalidOperationException("Amount must be strictly positive");
        }
    }
}

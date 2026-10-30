package com.bank.account.service;

import com.bank.account.domain.Account;
import com.bank.account.domain.AccountStatus;
import com.bank.account.exception.AccountNotFoundException;
import com.bank.account.exception.InvalidOperationException;
import com.bank.account.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account(AccountStatus status, double balance) {
        Account account = new Account();
        account.setCustomerReference("CUST-1");
        account.setCurrency("EUR");
        account.setStatus(status);
        account.setBalance(balance);
        return account;
    }

    @Test
    void createProducesActiveAccountWithUpperCaseCurrency() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = accountService.create("CUST-1", "eur", 100);

        assertEquals(AccountStatus.ACTIVE, created.getStatus());
        assertEquals("EUR", created.getCurrency());
        assertEquals(100, created.getBalance());
    }

    @Test
    void createRejectsNegativeInitialBalance() {
        assertThrows(InvalidOperationException.class, () -> accountService.create("CUST-1", "EUR", -1));
    }

    @Test
    void getThrowsWhenAccountIsMissing() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> accountService.get(id));
    }

    @Test
    void creditIncreasesBalance() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account(AccountStatus.ACTIVE, 50)));

        assertEquals(80, accountService.credit(id, 30).getBalance());
    }

    @Test
    void debitDecreasesBalance() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account(AccountStatus.ACTIVE, 50)));

        assertEquals(20, accountService.debit(id, 30).getBalance());
    }

    @Test
    void debitRejectsInsufficientBalance() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account(AccountStatus.ACTIVE, 10)));

        assertThrows(InvalidOperationException.class, () -> accountService.debit(id, 30));
    }

    @Test
    void blockedAccountCannotBeCreditedOrDebited() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account(AccountStatus.BLOCKED, 100)));

        assertThrows(InvalidOperationException.class, () -> accountService.credit(id, 10));
        assertThrows(InvalidOperationException.class, () -> accountService.debit(id, 10));
    }

    @Test
    void nonPositiveAmountIsRejected() {
        UUID id = UUID.randomUUID();

        assertThrows(InvalidOperationException.class, () -> accountService.credit(id, 0));
        assertThrows(InvalidOperationException.class, () -> accountService.debit(id, -5));
    }
}

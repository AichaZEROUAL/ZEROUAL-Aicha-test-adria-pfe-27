package com.bank.transaction.service;

import com.bank.transaction.client.AccountClient;
import com.bank.transaction.client.AccountDto;
import com.bank.transaction.client.AmountRequest;
import com.bank.transaction.config.TransactionProperties;
import com.bank.transaction.domain.Transaction;
import com.bank.transaction.exception.AccountNotFoundException;
import com.bank.transaction.exception.AccountServiceUnavailableException;
import com.bank.transaction.exception.InvalidTransactionException;
import com.bank.transaction.exception.TransactionNotFoundException;
import com.bank.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                transactionRepository, new TransactionProperties(new BigDecimal("1000")), accountClient);
    }

    private AccountDto account(UUID id, String status, String currency, double balance) {
        return new AccountDto(id, "CUST", status, currency, balance);
    }

    private CreateTransactionCommand command(UUID accountId, String amount) {
        return new CreateTransactionCommand(accountId, new BigDecimal(amount), "eur", "GROCERY", "FR");
    }

    private void stubSave() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createSavesTransactionAndDebitsAccount() {
        UUID accountId = UUID.randomUUID();
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "ACTIVE", "EUR", 500));
        stubSave();

        Transaction created = transactionService.create(command(accountId, "250.00"));

        assertEquals("EUR", created.getCurrency());
        assertEquals(new BigDecimal("250.00"), created.getAmount());
        assertNotNull(created.getCreatedAt());
        verify(accountClient).debit(accountId, new AmountRequest(250.0));
    }

    @Test
    void createAcceptsTheMaximumAmount() {
        UUID accountId = UUID.randomUUID();
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "ACTIVE", "EUR", 5000));
        stubSave();

        assertEquals(new BigDecimal("1000"), transactionService.create(command(accountId, "1000")).getAmount());
    }

    @Test
    void createRejectsAmountAboveMaximumWithoutCallingAccountService() {
        assertThrows(InvalidTransactionException.class, () -> transactionService.create(command(UUID.randomUUID(), "1000.01")));
        verifyNoInteractions(transactionRepository, accountClient);
    }

    @Test
    void createRejectsZeroAndNegativeAmounts() {
        assertThrows(InvalidTransactionException.class, () -> transactionService.create(command(UUID.randomUUID(), "0")));
        assertThrows(InvalidTransactionException.class, () -> transactionService.create(command(UUID.randomUUID(), "-5")));
    }

    @Test
    void createRejectsInactiveAccount() {
        UUID accountId = UUID.randomUUID();
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "BLOCKED", "EUR", 500));

        assertThrows(InvalidTransactionException.class, () -> transactionService.create(command(accountId, "10")));
        verify(transactionRepository, never()).save(any());
        verify(accountClient, never()).debit(any(), any());
    }

    @Test
    void createRejectsCurrencyMismatch() {
        UUID accountId = UUID.randomUUID();
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "ACTIVE", "MAD", 500));

        assertThrows(InvalidTransactionException.class, () -> transactionService.create(command(accountId, "10")));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createRejectsInsufficientBalance() {
        UUID accountId = UUID.randomUUID();
        when(accountClient.getAccount(accountId)).thenReturn(account(accountId, "ACTIVE", "EUR", 5));

        assertThrows(InvalidTransactionException.class, () -> transactionService.create(command(accountId, "10")));
        verify(accountClient, never()).debit(any(), any());
    }

    @Test
    void createPropagatesMissingAccount() {
        UUID accountId = UUID.randomUUID();
        when(accountClient.getAccount(accountId)).thenThrow(new AccountNotFoundException(accountId));

        assertThrows(AccountNotFoundException.class, () -> transactionService.create(command(accountId, "10")));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void createPropagatesAccountServiceOutage() {
        UUID accountId = UUID.randomUUID();
        when(accountClient.getAccount(accountId))
                .thenThrow(new AccountServiceUnavailableException("down", new RuntimeException()));

        assertThrows(AccountServiceUnavailableException.class, () -> transactionService.create(command(accountId, "10")));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void transferDebitsSourceAndCreditsTarget() {
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        when(accountClient.getAccount(source)).thenReturn(account(source, "ACTIVE", "EUR", 300));
        when(accountClient.getAccount(target)).thenReturn(account(target, "ACTIVE", "EUR", 10));
        stubSave();

        TransferResult result = transactionService.transfer(new TransferCommand(source, target, new BigDecimal("100"), "eur"));

        assertEquals(source, result.debit().getAccountId());
        assertEquals(target, result.credit().getAccountId());
        verify(accountClient).debit(source, new AmountRequest(100.0));
        verify(accountClient).credit(target, new AmountRequest(100.0));
    }

    @Test
    void transferRestoresSourceWhenCreditFails() {
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        when(accountClient.getAccount(source)).thenReturn(account(source, "ACTIVE", "EUR", 300));
        when(accountClient.getAccount(target)).thenReturn(account(target, "ACTIVE", "EUR", 10));
        when(accountClient.credit(target, new AmountRequest(100.0)))
                .thenThrow(new AccountServiceUnavailableException("down", new RuntimeException()));
        stubSave();

        assertThrows(AccountServiceUnavailableException.class,
                () -> transactionService.transfer(new TransferCommand(source, target, new BigDecimal("100"), "EUR")));

        verify(accountClient).debit(source, new AmountRequest(100.0));
        verify(accountClient).credit(source, new AmountRequest(100.0));
        verify(accountClient, times(2)).credit(any(), any());
    }

    @Test
    void transferRejectsSameAccount() {
        UUID account = UUID.randomUUID();

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.transfer(new TransferCommand(account, account, new BigDecimal("10"), "EUR")));
        verifyNoInteractions(accountClient);
    }

    @Test
    void transferRejectsInsufficientBalance() {
        UUID source = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        when(accountClient.getAccount(source)).thenReturn(account(source, "ACTIVE", "EUR", 5));
        when(accountClient.getAccount(target)).thenReturn(account(target, "ACTIVE", "EUR", 10));

        assertThrows(InvalidTransactionException.class,
                () -> transactionService.transfer(new TransferCommand(source, target, new BigDecimal("100"), "EUR")));
        verify(accountClient, never()).debit(any(), any());
    }

    @Test
    void getThrowsWhenTransactionIsMissing() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.get(id));
    }

    @Test
    void listByAccountDelegatesToRepository() {
        UUID accountId = UUID.randomUUID();

        transactionService.listByAccount(accountId);

        verify(transactionRepository).findByAccountIdOrderByCreatedAtDesc(accountId);
    }
}

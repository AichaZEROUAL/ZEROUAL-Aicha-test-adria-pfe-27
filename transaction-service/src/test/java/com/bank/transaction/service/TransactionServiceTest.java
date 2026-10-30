package com.bank.transaction.service;

import com.bank.transaction.config.TransactionProperties;
import com.bank.transaction.domain.Transaction;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, new TransactionProperties(new BigDecimal("1000")));
    }

    private CreateTransactionCommand command(String amount) {
        return new CreateTransactionCommand(UUID.randomUUID(), new BigDecimal(amount), "eur", "GROCERY", "FR");
    }

    @Test
    void createStoresTransactionWithTimestampAndUpperCaseCurrency() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction created = transactionService.create(command("250.00"));

        assertEquals("EUR", created.getCurrency());
        assertEquals(new BigDecimal("250.00"), created.getAmount());
        assertNotNull(created.getCreatedAt());
    }

    @Test
    void createAcceptsTheMaximumAmount() {
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(new BigDecimal("1000"), transactionService.create(command("1000")).getAmount());
    }

    @Test
    void createRejectsAmountAboveMaximum() {
        assertThrows(InvalidTransactionException.class, () -> transactionService.create(command("1000.01")));
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void createRejectsZeroAndNegativeAmounts() {
        assertThrows(InvalidTransactionException.class, () -> transactionService.create(command("0")));
        assertThrows(InvalidTransactionException.class, () -> transactionService.create(command("-5")));
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

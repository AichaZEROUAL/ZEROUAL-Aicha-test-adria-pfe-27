package com.bank.transaction.web.graphql;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionInput(
        UUID accountId,
        BigDecimal amount,
        String currency,
        String merchantCategory,
        String country) {
}

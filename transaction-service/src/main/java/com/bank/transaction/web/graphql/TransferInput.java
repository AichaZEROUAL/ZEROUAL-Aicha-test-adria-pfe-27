package com.bank.transaction.web.graphql;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferInput(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount, String currency) {
}

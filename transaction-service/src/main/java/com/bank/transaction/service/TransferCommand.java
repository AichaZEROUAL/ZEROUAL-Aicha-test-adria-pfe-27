package com.bank.transaction.service;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCommand(UUID sourceAccountId, UUID targetAccountId, BigDecimal amount, String currency) {
}

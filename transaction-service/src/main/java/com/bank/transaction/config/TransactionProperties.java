package com.bank.transaction.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "bank.transaction")
public record TransactionProperties(@NotNull @Positive BigDecimal maxAmount) {
}

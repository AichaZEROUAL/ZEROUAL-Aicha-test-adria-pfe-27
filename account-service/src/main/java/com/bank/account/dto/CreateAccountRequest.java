package com.bank.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateAccountRequest(
        @NotBlank String customerReference,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @PositiveOrZero Double initialBalance) {

    public double initialBalanceOrZero() {
        return initialBalance == null ? 0 : initialBalance;
    }
}

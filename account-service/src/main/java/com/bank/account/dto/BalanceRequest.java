package com.bank.account.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record BalanceRequest(@NotNull @PositiveOrZero Double balance) {
}

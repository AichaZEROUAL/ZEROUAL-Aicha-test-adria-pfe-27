package com.bank.transaction.client;

import java.util.UUID;

public record AccountDto(UUID id, String customerReference, String status, String currency, double balance) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}

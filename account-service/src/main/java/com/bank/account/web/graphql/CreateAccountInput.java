package com.bank.account.web.graphql;

public record CreateAccountInput(String customerReference, String currency, Double initialBalance) {
}

package com.bank.transaction.service;

import com.bank.transaction.domain.Transaction;

public record TransferResult(Transaction debit, Transaction credit) {
}

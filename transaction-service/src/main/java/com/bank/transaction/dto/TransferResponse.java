package com.bank.transaction.dto;

import com.bank.transaction.service.TransferResult;

public record TransferResponse(TransactionResponse debit, TransactionResponse credit) {

    public static TransferResponse from(TransferResult result) {
        return new TransferResponse(TransactionResponse.from(result.debit()), TransactionResponse.from(result.credit()));
    }
}

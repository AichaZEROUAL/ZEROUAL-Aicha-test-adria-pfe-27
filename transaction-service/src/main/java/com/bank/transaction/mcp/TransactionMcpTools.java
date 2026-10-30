package com.bank.transaction.mcp;

import com.bank.transaction.dto.TransactionResponse;
import com.bank.transaction.dto.TransferResponse;
import com.bank.transaction.service.CreateTransactionCommand;
import com.bank.transaction.service.TransactionService;
import com.bank.transaction.service.TransferCommand;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class TransactionMcpTools {

    private final TransactionService transactionService;

    public TransactionMcpTools(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @McpTool(name = "create_transaction", description = "Record a new bank transaction on an account")
    public TransactionResponse createTransaction(
            @McpToolParam(description = "Account UUID", required = true) String accountId,
            @McpToolParam(description = "Amount, strictly positive and below the configured maximum", required = true) double amount,
            @McpToolParam(description = "ISO currency code such as EUR or MAD", required = true) String currency,
            @McpToolParam(description = "Merchant category such as GROCERY or TRAVEL", required = true) String merchantCategory,
            @McpToolParam(description = "Country code such as FR or MA", required = true) String country) {
        return TransactionResponse.from(transactionService.create(new CreateTransactionCommand(
                UUID.fromString(accountId),
                BigDecimal.valueOf(amount),
                currency,
                merchantCategory,
                country)));
    }

    @McpTool(name = "transfer_money", description = "Transfer money from one account to another account in the same currency")
    public TransferResponse transferMoney(
            @McpToolParam(description = "Source account UUID", required = true) String sourceAccountId,
            @McpToolParam(description = "Target account UUID", required = true) String targetAccountId,
            @McpToolParam(description = "Amount, strictly positive and below the configured maximum", required = true) double amount,
            @McpToolParam(description = "ISO currency code such as EUR or MAD", required = true) String currency) {
        return TransferResponse.from(transactionService.transfer(new TransferCommand(
                UUID.fromString(sourceAccountId),
                UUID.fromString(targetAccountId),
                BigDecimal.valueOf(amount),
                currency)));
    }

    @McpTool(name = "get_transaction", description = "Get a transaction by its id")
    public TransactionResponse getTransaction(
            @McpToolParam(description = "Transaction UUID", required = true) String transactionId) {
        return TransactionResponse.from(transactionService.get(UUID.fromString(transactionId)));
    }

    @McpTool(name = "list_transactions", description = "List all transactions")
    public List<TransactionResponse> listTransactions() {
        return transactionService.list().stream().map(TransactionResponse::from).toList();
    }

    @McpTool(name = "list_account_transactions", description = "List the transactions of one account, newest first")
    public List<TransactionResponse> listAccountTransactions(
            @McpToolParam(description = "Account UUID", required = true) String accountId) {
        return transactionService.listByAccount(UUID.fromString(accountId)).stream()
                .map(TransactionResponse::from).toList();
    }
}

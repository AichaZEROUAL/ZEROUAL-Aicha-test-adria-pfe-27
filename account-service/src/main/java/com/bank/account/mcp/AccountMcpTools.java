package com.bank.account.mcp;

import com.bank.account.domain.AccountStatus;
import com.bank.account.dto.AccountResponse;
import com.bank.account.service.AccountService;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AccountMcpTools {

    private final AccountService accountService;

    public AccountMcpTools(AccountService accountService) {
        this.accountService = accountService;
    }

    @McpTool(name = "list_accounts", description = "List all bank accounts")
    public List<AccountResponse> listAccounts() {
        return accountService.list().stream().map(AccountResponse::from).toList();
    }

    @McpTool(name = "get_account", description = "Get a bank account by its id, including its balance and status")
    public AccountResponse getAccount(
            @McpToolParam(description = "Account UUID", required = true) String accountId) {
        return AccountResponse.from(accountService.get(UUID.fromString(accountId)));
    }

    @McpTool(name = "create_account", description = "Create a new active bank account")
    public AccountResponse createAccount(
            @McpToolParam(description = "Customer reference", required = true) String customerReference,
            @McpToolParam(description = "ISO currency code such as EUR or MAD", required = true) String currency,
            @McpToolParam(description = "Initial balance, 0 when omitted", required = false) Double initialBalance) {
        double balance = initialBalance == null ? 0 : initialBalance;
        return AccountResponse.from(accountService.create(customerReference, currency, balance));
    }

    @McpTool(name = "update_account_status", description = "Block or activate a bank account")
    public AccountResponse updateAccountStatus(
            @McpToolParam(description = "Account UUID", required = true) String accountId,
            @McpToolParam(description = "ACTIVE or BLOCKED", required = true) String status) {
        return AccountResponse.from(accountService.updateStatus(UUID.fromString(accountId), AccountStatus.valueOf(status)));
    }

    @McpTool(name = "credit_account", description = "Add money to an active account")
    public AccountResponse creditAccount(
            @McpToolParam(description = "Account UUID", required = true) String accountId,
            @McpToolParam(description = "Amount to add, strictly positive", required = true) double amount) {
        return AccountResponse.from(accountService.credit(UUID.fromString(accountId), amount));
    }

    @McpTool(name = "debit_account", description = "Withdraw money from an active account with sufficient balance")
    public AccountResponse debitAccount(
            @McpToolParam(description = "Account UUID", required = true) String accountId,
            @McpToolParam(description = "Amount to withdraw, strictly positive", required = true) double amount) {
        return AccountResponse.from(accountService.debit(UUID.fromString(accountId), amount));
    }
}

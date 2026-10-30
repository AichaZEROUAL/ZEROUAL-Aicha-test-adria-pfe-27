package com.bank.account.web.graphql;

import com.bank.account.domain.AccountStatus;
import com.bank.account.dto.AccountResponse;
import com.bank.account.exception.AccountNotFoundException;
import com.bank.account.service.AccountService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class AccountGraphQLController {

    private final AccountService accountService;

    public AccountGraphQLController(AccountService accountService) {
        this.accountService = accountService;
    }

    @QueryMapping
    public List<AccountResponse> accounts() {
        return accountService.list().stream().map(AccountResponse::from).toList();
    }

    @QueryMapping
    public AccountResponse accountById(@Argument UUID id) {
        try {
            return AccountResponse.from(accountService.get(id));
        } catch (AccountNotFoundException exception) {
            return null;
        }
    }

    @MutationMapping
    public AccountResponse createAccount(@Argument CreateAccountInput input) {
        double initialBalance = input.initialBalance() == null ? 0 : input.initialBalance();
        return AccountResponse.from(accountService.create(input.customerReference(), input.currency(), initialBalance));
    }

    @MutationMapping
    public AccountResponse updateAccountStatus(@Argument UUID id, @Argument AccountStatus status) {
        return AccountResponse.from(accountService.updateStatus(id, status));
    }

    @MutationMapping
    public AccountResponse creditAccount(@Argument UUID id, @Argument double amount) {
        return AccountResponse.from(accountService.credit(id, amount));
    }

    @MutationMapping
    public AccountResponse debitAccount(@Argument UUID id, @Argument double amount) {
        return AccountResponse.from(accountService.debit(id, amount));
    }
}

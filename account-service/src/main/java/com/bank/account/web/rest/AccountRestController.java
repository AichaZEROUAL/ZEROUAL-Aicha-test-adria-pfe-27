package com.bank.account.web.rest;

import com.bank.account.dto.AccountResponse;
import com.bank.account.dto.AmountRequest;
import com.bank.account.dto.BalanceRequest;
import com.bank.account.dto.CreateAccountRequest;
import com.bank.account.dto.UpdateStatusRequest;
import com.bank.account.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountRestController {

    private final AccountService accountService;

    public AccountRestController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(accountService.create(
                request.customerReference(), request.currency(), request.initialBalanceOrZero()));
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.list().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable UUID id) {
        return AccountResponse.from(accountService.get(id));
    }

    @PatchMapping("/{id}/status")
    public AccountResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
        return AccountResponse.from(accountService.updateStatus(id, request.status()));
    }

    @PutMapping("/{id}/balance")
    public AccountResponse updateBalance(@PathVariable UUID id, @Valid @RequestBody BalanceRequest request) {
        return AccountResponse.from(accountService.updateBalance(id, request.balance()));
    }

    @PostMapping("/{id}/credit")
    public AccountResponse credit(@PathVariable UUID id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(accountService.credit(id, request.amount()));
    }

    @PostMapping("/{id}/debit")
    public AccountResponse debit(@PathVariable UUID id, @Valid @RequestBody AmountRequest request) {
        return AccountResponse.from(accountService.debit(id, request.amount()));
    }
}

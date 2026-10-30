package com.bank.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "account-service",
        url = "${bank.account-service.url:}",
        fallbackFactory = AccountClientFallbackFactory.class,
        configuration = AccountClientConfiguration.class)
public interface AccountClient {

    @GetMapping("/api/accounts/{id}")
    AccountDto getAccount(@PathVariable("id") UUID id);

    @PostMapping("/api/accounts/{id}/debit")
    AccountDto debit(@PathVariable("id") UUID id, @RequestBody AmountRequest request);

    @PostMapping("/api/accounts/{id}/credit")
    AccountDto credit(@PathVariable("id") UUID id, @RequestBody AmountRequest request);
}

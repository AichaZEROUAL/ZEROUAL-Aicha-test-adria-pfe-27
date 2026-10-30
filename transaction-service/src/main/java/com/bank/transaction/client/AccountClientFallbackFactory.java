package com.bank.transaction.client;

import com.bank.transaction.exception.AccountNotFoundException;
import com.bank.transaction.exception.AccountServiceUnavailableException;
import com.bank.transaction.exception.InvalidTransactionException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountClientFallbackFactory implements FallbackFactory<AccountClient> {

    private static final Logger log = LoggerFactory.getLogger(AccountClientFallbackFactory.class);

    @Override
    public AccountClient create(Throwable cause) {
        log.warn("Circuit breaker fallback triggered for account-service: {}", cause.toString());
        return new AccountClientFallback(cause);
    }

    static class AccountClientFallback implements AccountClient {

        private final Throwable cause;

        AccountClientFallback(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public AccountDto getAccount(UUID id) {
            throw translate(id);
        }

        @Override
        public AccountDto debit(UUID id, AmountRequest request) {
            throw translate(id);
        }

        @Override
        public AccountDto credit(UUID id, AmountRequest request) {
            throw translate(id);
        }

        private RuntimeException translate(UUID id) {
            if (cause instanceof FeignException feignException) {
                int status = feignException.status();
                if (status == 404) {
                    return new AccountNotFoundException(id);
                }
                if (status >= 400 && status < 500) {
                    return new InvalidTransactionException("Account service rejected the operation on account " + id + " (HTTP " + status + ")");
                }
            }
            return new AccountServiceUnavailableException("Account service is unavailable, please retry later", cause);
        }
    }
}

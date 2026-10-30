package com.bank.transaction.client;

import com.bank.transaction.exception.AccountNotFoundException;
import com.bank.transaction.exception.AccountServiceUnavailableException;
import com.bank.transaction.exception.InvalidTransactionException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountClientFallbackFactoryTest {

    private final AccountClientFallbackFactory factory = new AccountClientFallbackFactory();
    private final UUID id = UUID.randomUUID();

    private FeignException feignException(int status) {
        Request request = Request.create(Request.HttpMethod.GET, "http://account-service/api/accounts/" + id,
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(status).reason("reason").request(request).headers(Map.of()).build();
        return FeignException.errorStatus("AccountClient#getAccount", response);
    }

    @Test
    void notFoundIsTranslatedToAccountNotFound() {
        AccountClient fallback = factory.create(feignException(404));

        assertThrows(AccountNotFoundException.class, () -> fallback.getAccount(id));
    }

    @Test
    void clientErrorsAreTranslatedToInvalidTransaction() {
        AccountClient fallback = factory.create(feignException(409));

        assertThrows(InvalidTransactionException.class, () -> fallback.debit(id, new AmountRequest(5)));
    }

    @Test
    void serverErrorsAreTranslatedToUnavailable() {
        AccountClient fallback = factory.create(feignException(500));

        assertThrows(AccountServiceUnavailableException.class, () -> fallback.credit(id, new AmountRequest(5)));
    }

    @Test
    void timeoutsAreTranslatedToUnavailable() {
        AccountClient fallback = factory.create(new TimeoutException("slow"));

        assertThrows(AccountServiceUnavailableException.class, () -> fallback.getAccount(id));
    }
}

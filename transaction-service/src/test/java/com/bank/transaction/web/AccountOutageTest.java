package com.bank.transaction.web;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.config.import=",
        "bank.transaction.max-amount=1000",
        "bank.account-service.url=http://localhost:1"
})
@AutoConfigureMockMvc
class AccountOutageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void transactionsFailFastWith503WhileAccountServiceIsDownAndNothingIsStored() throws Exception {
        UUID accountId = UUID.randomUUID();
        String body = "{\"accountId\":\"" + accountId
                + "\",\"amount\":10,\"currency\":\"EUR\",\"merchantCategory\":\"GROCERY\",\"country\":\"FR\"}";

        for (int attempt = 0; attempt < 8; attempt++) {
            mockMvc.perform(post("/api/transactions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isServiceUnavailable());
        }

        assertTrue(circuitBreakerRegistry.getAllCircuitBreakers().stream()
                .anyMatch(breaker -> breaker.getState() == CircuitBreaker.State.OPEN));

        mockMvc.perform(get("/api/transactions").param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}

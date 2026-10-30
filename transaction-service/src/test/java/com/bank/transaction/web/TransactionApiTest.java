package com.bank.transaction.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.config.import="
})
@AutoConfigureMockMvc
class TransactionApiTest {

    @Autowired
    private MockMvc mockMvc;

    private String body(UUID accountId, String amount) {
        return "{\"accountId\":\"" + accountId + "\",\"amount\":" + amount
                + ",\"currency\":\"eur\",\"merchantCategory\":\"GROCERY\",\"country\":\"FR\"}";
    }

    private String createTransaction(UUID accountId, String amount) throws Exception {
        String response = mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(accountId, amount)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void createAndGetTransaction() throws Exception {
        UUID accountId = UUID.randomUUID();
        String id = createTransaction(accountId, "250.00");

        mockMvc.perform(get("/api/transactions/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.amount").value(250.0))
                .andExpect(jsonPath("$.accountId").value(accountId.toString()));
    }

    @Test
    void amountAboveConfiguredMaximumIsRejected() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID(), "1000.50")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void negativeAmountIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID(), "-10")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTransactionReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/transactions/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsTransactionsOfOneAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        createTransaction(accountId, "10");
        createTransaction(accountId, "20");
        createTransaction(UUID.randomUUID(), "30");

        mockMvc.perform(get("/api/transactions").param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void graphqlCreatesAndReadsTransaction() throws Exception {
        UUID accountId = UUID.randomUUID();
        String mutation = "{\"query\":\"mutation { createTransaction(input: {accountId: \\\"" + accountId
                + "\\\", amount: 99.5, currency: \\\"EUR\\\", merchantCategory: \\\"TRAVEL\\\", country: \\\"MA\\\"}) { id amount currency } }\"}";
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mutation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createTransaction.amount").value(99.5));

        String query = "{\"query\":\"{ transactionsByAccount(accountId: \\\"" + accountId + "\\\") { merchantCategory } }\"}";
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactionsByAccount[0].merchantCategory").value("TRAVEL"));
    }
}

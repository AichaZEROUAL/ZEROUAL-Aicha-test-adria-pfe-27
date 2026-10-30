package com.bank.account.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.config.import="
})
@AutoConfigureMockMvc
class AccountApiTest {

    @Autowired
    private MockMvc mockMvc;

    private String createAccount(String body) throws Exception {
        String response = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Test
    void createAndGetAccount() throws Exception {
        String id = createAccount("{\"customerReference\":\"CUST-1\",\"currency\":\"eur\",\"initialBalance\":100}");

        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.balance").value(100.0));
    }

    @Test
    void invalidCurrencyIsRejected() throws Exception {
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerReference\":\"CUST-1\",\"currency\":\"EURO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAccountReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void debitBeyondBalanceReturnsConflict() throws Exception {
        String id = createAccount("{\"customerReference\":\"CUST-2\",\"currency\":\"MAD\",\"initialBalance\":10}");

        mockMvc.perform(post("/api/accounts/" + id + "/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":50}"))
                .andExpect(status().isConflict());
    }

    @Test
    void creditAndBlockAccount() throws Exception {
        String id = createAccount("{\"customerReference\":\"CUST-3\",\"currency\":\"EUR\",\"initialBalance\":10}");

        mockMvc.perform(post("/api/accounts/" + id + "/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(25.0));

        mockMvc.perform(patch("/api/accounts/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(post("/api/accounts/" + id + "/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":5}"))
                .andExpect(status().isConflict());
    }

    @Test
    void graphqlCreatesAndReadsAccount() throws Exception {
        String mutation = "{\"query\":\"mutation { createAccount(input: {customerReference: \\\"CUST-G\\\", currency: \\\"EUR\\\", initialBalance: 42}) { id status balance } }\"}";
        String response = mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mutation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.createAccount.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(response, "$.data.createAccount.id");

        String query = "{\"query\":\"{ accountById(id: \\\"" + id + "\\\") { customerReference balance } }\"}";
        mockMvc.perform(post("/graphql")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(query))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CUST-G")))
                .andExpect(jsonPath("$.data.accountById.balance").value(42.0));
    }
}

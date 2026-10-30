package com.bank.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class ConfigServiceTest {

    @LocalServerPort
    private int port;

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json").GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void sharedPropertyIsServedToAnyApplication() throws Exception {
        HttpResponse<String> response = get("/any-service/default");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"bank.transaction.max-amount\":\"1000\""));
    }

    @Test
    void sharedPropertyIsServedToTransactionService() throws Exception {
        HttpResponse<String> response = get("/transaction-service/default");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"bank.transaction.max-amount\":\"1000\""));
    }

    @Test
    void propertyIsAlsoAvailableAsFlatPropertiesFile() throws Exception {
        HttpResponse<String> response = get("/transaction-service-default.properties");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("bank.transaction.max-amount: 1000")
                || response.body().contains("bank.transaction.max-amount=1000"));
    }
}

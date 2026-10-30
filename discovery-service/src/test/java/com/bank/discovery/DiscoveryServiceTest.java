package com.bank.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServiceTest {

    private static final String INSTANCE = """
            {"instance":{"instanceId":"test-1","hostName":"localhost","app":"TEST-SERVICE","ipAddr":"127.0.0.1",
            "status":"UP","port":{"$":9000,"@enabled":"true"},
            "dataCenterInfo":{"@class":"com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo","name":"MyOwn"}}}
            """;

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    @Test
    void dashboardIsServed() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/")).GET());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Eureka"));
    }

    @Test
    void emptyRegistryIsExposed() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/eureka/apps"))
                .header("Accept", "application/json").GET());

        assertEquals(200, response.statusCode());
    }

    @Test
    void registeredInstanceCanBeDiscovered() throws Exception {
        HttpResponse<String> registration = send(HttpRequest.newBuilder(uri("/eureka/apps/TEST-SERVICE"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(INSTANCE)));
        assertEquals(204, registration.statusCode());

        HttpResponse<String> lookup = send(HttpRequest.newBuilder(uri("/eureka/apps/TEST-SERVICE"))
                .header("Accept", "application/json").GET());

        assertEquals(200, lookup.statusCode());
        assertTrue(lookup.body().contains("test-1"));
    }

    @Test
    void unknownApplicationIsNotFound() throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri("/eureka/apps/UNKNOWN-SERVICE"))
                .header("Accept", "application/json").GET());

        assertEquals(404, response.statusCode());
    }
}

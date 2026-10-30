package com.bank.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "spring.config.import=",
        "spring.cloud.gateway.server.webflux.discovery.locator.enabled=false"
})
@AutoConfigureWebTestClient
class GatewayRoutingTest {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private WebTestClient webTestClient;

    private Route route(String id) {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        return routes.stream().filter(route -> route.getId().equals(id)).findFirst().orElse(null);
    }

    @Test
    void accountServiceRouteTargetsTheLoadBalancedService() {
        Route route = route("account-service");

        assertTrue(route != null);
        assertEquals("lb://account-service", route.getUri().toString());
    }

    @Test
    void transactionServiceRouteTargetsTheLoadBalancedService() {
        Route route = route("transaction-service");

        assertTrue(route != null);
        assertEquals("lb://transaction-service", route.getUri().toString());
    }

    @Test
    void preflightFromTheAngularOriginIsAllowed() {
        webTestClient.options().uri("/api/accounts")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("Access-Control-Allow-Origin", "http://localhost:4200");
    }

    @Test
    void preflightFromAnotherOriginIsRejected() {
        webTestClient.options().uri("/api/accounts")
                .header("Origin", "http://evil.example")
                .header("Access-Control-Request-Method", "POST")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void unknownPathReturnsNotFound() {
        webTestClient.get().uri("/api/unknown")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void routedPathWithoutAvailableInstanceReturnsServiceUnavailable() {
        webTestClient.get().uri("/api/accounts")
                .exchange()
                .expectStatus().isEqualTo(503);
    }
}

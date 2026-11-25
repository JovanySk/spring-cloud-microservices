package com.jovany.bakery.gateway.console;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

/**
 * The status endpoint with a mocked DiscoveryClient and a fake health probe:
 * no Eureka and no running services are needed.
 */
class ConsoleStatusControllerTests {

    private static final URI EUREKA = URI.create("http://eureka.test:8761");
    private static final URI CONFIG = URI.create("http://config.test:8888");
    private static final URI CATALOG = URI.create("http://10.0.0.1:8081");
    private static final URI INVENTORY = URI.create("http://10.0.0.2:8082");
    private static final URI ORDER = URI.create("http://10.0.0.3:8083");

    private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
    private final Map<URI, Mono<String>> health = new HashMap<>();
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        HealthProbe probe = uri -> health.getOrDefault(uri, Mono.just("DOWN"));
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        ConsoleStatusService service = new ConsoleStatusService(discoveryClient, probe,
                List.of("catalog-service", "inventory-service", "order-service"), EUREKA, CONFIG, clock);
        client = WebTestClient.bindToController(new ConsoleStatusController(service)).build();

        register("catalog-service", CATALOG);
        register("inventory-service", INVENTORY);
        register("order-service", ORDER);
        List.of(EUREKA, CONFIG, CATALOG, INVENTORY, ORDER).forEach(uri -> health.put(uri, Mono.just("UP")));
    }

    private void register(String serviceId, URI uri) {
        when(discoveryClient.getInstances(serviceId)).thenReturn(List.of(
                new DefaultServiceInstance(serviceId + "-1", serviceId, uri.getHost(), uri.getPort(), false)));
    }

    private WebTestClient.BodyContentSpec getStatus() {
        return client.get().uri("/console/api/status")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectBody();
    }

    @Test
    void reportsEverythingUpWhenAllInstancesAreHealthy() {
        getStatus()
                .jsonPath("$.overall").isEqualTo("UP")
                .jsonPath("$.checkedAt").isEqualTo("2026-01-01T10:00:00Z")
                .jsonPath("$.gateway.status").isEqualTo("UP")
                .jsonPath("$.infrastructure[0].name").isEqualTo("discovery-server")
                .jsonPath("$.infrastructure[0].status").isEqualTo("UP")
                .jsonPath("$.infrastructure[1].name").isEqualTo("config-server")
                .jsonPath("$.infrastructure[1].status").isEqualTo("UP")
                .jsonPath("$.services.length()").isEqualTo(3)
                .jsonPath("$.services[0].name").isEqualTo("catalog-service")
                .jsonPath("$.services[0].status").isEqualTo("UP")
                .jsonPath("$.services[0].instances[0].instanceId").isEqualTo("catalog-service-1")
                .jsonPath("$.services[0].instances[0].uri").isEqualTo("http://10.0.0.1:8081")
                .jsonPath("$.services[1].status").isEqualTo("UP")
                .jsonPath("$.services[2].status").isEqualTo("UP");
    }

    @Test
    void serviceIsDownWhenItsHealthCheckFailsEvenIfStillRegistered() {
        health.put(INVENTORY, Mono.error(new IllegalStateException("connection refused")));

        getStatus()
                .jsonPath("$.overall").isEqualTo("DEGRADED")
                .jsonPath("$.services[1].name").isEqualTo("inventory-service")
                .jsonPath("$.services[1].status").isEqualTo("DOWN")
                .jsonPath("$.services[1].instances[0].status").isEqualTo("DOWN")
                .jsonPath("$.services[0].status").isEqualTo("UP");
    }

    @Test
    void serviceIsDownWhenNoInstanceIsRegistered() {
        when(discoveryClient.getInstances("order-service")).thenReturn(List.of());

        getStatus()
                .jsonPath("$.overall").isEqualTo("DEGRADED")
                .jsonPath("$.services[2].name").isEqualTo("order-service")
                .jsonPath("$.services[2].status").isEqualTo("DOWN")
                .jsonPath("$.services[2].instances.length()").isEqualTo(0);
    }

    @Test
    void serviceIsUpWhileAtLeastOneInstanceIsHealthy() {
        URI secondCatalog = URI.create("http://10.0.0.9:9081");
        when(discoveryClient.getInstances("catalog-service")).thenReturn(List.of(
                new DefaultServiceInstance("catalog-service-1", "catalog-service", "10.0.0.1", 8081, false),
                new DefaultServiceInstance("catalog-service-2", "catalog-service", "10.0.0.9", 9081, false)));
        health.put(CATALOG, Mono.just("DOWN"));
        health.put(secondCatalog, Mono.just("UP"));

        getStatus()
                .jsonPath("$.services[0].status").isEqualTo("UP")
                .jsonPath("$.services[0].instances.length()").isEqualTo(2);
    }

    @Test
    void infrastructureIsDownWhenItsHealthCannotBeRead() {
        health.put(CONFIG, Mono.error(new IllegalStateException("timeout")));

        getStatus()
                .jsonPath("$.overall").isEqualTo("DEGRADED")
                .jsonPath("$.infrastructure[1].name").isEqualTo("config-server")
                .jsonPath("$.infrastructure[1].url").isEqualTo("http://config.test:8888")
                .jsonPath("$.infrastructure[1].status").isEqualTo("DOWN");
    }

    @Test
    void discoveryErrorsAreReportedAsDownInsteadOfFailingTheRequest() {
        when(discoveryClient.getInstances("catalog-service")).thenThrow(new IllegalStateException("registry"));

        getStatus()
                .jsonPath("$.services[0].status").isEqualTo("DOWN")
                .jsonPath("$.services[1].status").isEqualTo("UP");
    }
}

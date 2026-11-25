package com.jovany.bakery.gateway.console;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Full gateway context: the console page and its status endpoint are served by the gateway
 * itself and are not swallowed by the /api/** routes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // Nothing listens here, so the probes fail fast and deterministically
                "console.eureka-url=http://127.0.0.1:1",
                "console.config-server-url=http://127.0.0.1:1",
                "console.health-timeout=500ms"
        })
class ConsolePageTests {

    @Autowired
    private WebTestClient client;

    @Test
    void servesTheConsolePageAtTheRoot() {
        client.get().uri("/")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> assertThat(html)
                        .contains("console/console.js")
                        .contains("id=\"map\""));
    }

    @Test
    void statusEndpointAnswersWithoutEurekaAndReportsServicesDown() {
        client.get().uri("/console/api/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.gateway.status").isEqualTo("UP")
                .jsonPath("$.overall").isEqualTo("DEGRADED")
                .jsonPath("$.infrastructure[0].status").isEqualTo("DOWN")
                .jsonPath("$.services.length()").isEqualTo(3)
                .jsonPath("$.services[0].status").isEqualTo("DOWN");
    }
}

package com.jovany.bakery.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Starts the real Config Server against the config-repo folder of this repository
 * (Maven runs the tests from the module folder, hence "../config-repo").
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.config.server.native.search-locations=file:../config-repo/")
class ConfigServerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void servesDefaultConfigurationForCatalogService() {
        ResponseEntity<String> response = restTemplate.getForEntity("/catalog-service/default", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("catalog-service.yml")
                .contains("\"server.port\":8081")
                .contains("jdbc:h2:mem:catalogdb");
    }

    @Test
    void servesDockerOverridesWhenDockerProfileIsRequested() {
        ResponseEntity<String> response = restTemplate.getForEntity("/order-service/docker", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("application-docker.yml")
                .contains("http://discovery-server:8761/eureka/");
    }

    @Test
    void healthEndpointIsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }
}

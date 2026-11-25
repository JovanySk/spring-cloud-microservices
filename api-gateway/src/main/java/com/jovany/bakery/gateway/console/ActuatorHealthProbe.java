package com.jovany.bakery.gateway.console;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Reads GET {baseUri}/actuator/health with a short timeout. A 503 from actuator still carries
 * a body with the real status, so it is parsed too; anything else counts as DOWN.
 */
@Component
class ActuatorHealthProbe implements HealthProbe {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP =
            new ParameterizedTypeReference<>() { };

    private final WebClient webClient;
    private final Duration timeout;

    ActuatorHealthProbe(WebClient.Builder builder,
                        @Value("${console.health-timeout:1500ms}") Duration timeout) {
        this.webClient = builder.build();
        this.timeout = timeout;
    }

    @Override
    public Mono<String> check(URI baseUri) {
        URI healthUri = baseUri.resolve("/actuator/health");
        return webClient.get()
                .uri(healthUri)
                .retrieve()
                .bodyToMono(JSON_MAP)
                .map(ActuatorHealthProbe::statusOf)
                .timeout(timeout)
                .onErrorResume(WebClientResponseException.class, ex -> Mono.just(statusOf(ex)))
                .onErrorReturn("DOWN")
                .defaultIfEmpty("DOWN");
    }

    private static String statusOf(Map<String, Object> body) {
        Object status = body.get("status");
        return status == null ? "DOWN" : status.toString();
    }

    private static String statusOf(WebClientResponseException ex) {
        try {
            Map<?, ?> body = ex.getResponseBodyAs(Map.class);
            Object status = body == null ? null : body.get("status");
            return status == null ? "DOWN" : status.toString();
        } catch (RuntimeException parseError) {
            return "DOWN";
        }
    }
}

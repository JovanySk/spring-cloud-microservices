package com.jovany.bakery.gateway.console;

import java.net.URI;
import reactor.core.publisher.Mono;

/**
 * Asks a running process for its health. Implementations must never fail: any error,
 * timeout or unexpected answer is reported as "DOWN".
 */
public interface HealthProbe {

    /**
     * @param baseUri base address of the process, e.g. http://192.168.1.10:8081
     * @return the status reported by its /actuator/health endpoint ("UP", "DOWN", ...)
     */
    Mono<String> check(URI baseUri);
}

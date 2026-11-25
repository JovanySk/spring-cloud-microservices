package com.jovany.bakery.gateway.console;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Read-only endpoint used by the ops console (static page served at "/").
 * It is a normal WebFlux controller, so it is matched before the gateway routes.
 */
@RestController
public class ConsoleStatusController {

    private final ConsoleStatusService statusService;

    public ConsoleStatusController(ConsoleStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/console/api/status")
    public Mono<ResponseEntity<SystemStatus>> status() {
        return statusService.currentStatus()
                .map(status -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .body(status));
    }
}

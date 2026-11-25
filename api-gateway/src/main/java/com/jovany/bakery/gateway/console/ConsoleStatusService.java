package com.jovany.bakery.gateway.console;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Builds the status snapshot for the ops console. It only reads: it asks the DiscoveryClient
 * (Eureka) which instances are registered and calls /actuator/health on each of them, plus on
 * Eureka and the Config Server themselves.
 *
 * Eureka can still list an instance for a few seconds after its process died, so the health
 * call (not the registry) decides whether an instance is UP.
 */
@Service
public class ConsoleStatusService {

    static final String UP = "UP";
    static final String DOWN = "DOWN";

    private final DiscoveryClient discoveryClient;
    private final HealthProbe healthProbe;
    private final List<String> serviceIds;
    private final URI eurekaUrl;
    private final URI configServerUrl;
    private final Clock clock;

    @Autowired
    public ConsoleStatusService(DiscoveryClient discoveryClient,
                                HealthProbe healthProbe,
                                @Value("${console.services:catalog-service,inventory-service,order-service}")
                                List<String> serviceIds,
                                @Value("${console.eureka-url:http://localhost:8761}") URI eurekaUrl,
                                @Value("${console.config-server-url:http://localhost:8888}") URI configServerUrl) {
        this(discoveryClient, healthProbe, serviceIds, eurekaUrl, configServerUrl, Clock.systemUTC());
    }

    ConsoleStatusService(DiscoveryClient discoveryClient, HealthProbe healthProbe, List<String> serviceIds,
                         URI eurekaUrl, URI configServerUrl, Clock clock) {
        this.discoveryClient = discoveryClient;
        this.healthProbe = healthProbe;
        this.serviceIds = List.copyOf(serviceIds);
        this.eurekaUrl = eurekaUrl;
        this.configServerUrl = configServerUrl;
        this.clock = clock;
    }

    public Mono<SystemStatus> currentStatus() {
        Mono<List<SystemStatus.Node>> infrastructure = Flux.concat(
                        probeNode("discovery-server", eurekaUrl),
                        probeNode("config-server", configServerUrl))
                .collectList();

        Mono<List<SystemStatus.Service>> services = Flux.fromIterable(serviceIds)
                .concatMap(this::serviceStatus)
                .collectList();

        return Mono.zip(infrastructure, services).map(tuple -> {
            List<SystemStatus.Node> infra = tuple.getT1();
            List<SystemStatus.Service> business = tuple.getT2();
            boolean allUp = infra.stream().allMatch(node -> UP.equals(node.status()))
                    && business.stream().allMatch(service -> UP.equals(service.status()));
            return new SystemStatus(
                    Instant.now(clock).toString(),
                    allUp ? UP : "DEGRADED",
                    new SystemStatus.Node("api-gateway", null, UP),
                    infra,
                    business);
        });
    }

    private Mono<SystemStatus.Node> probeNode(String name, URI url) {
        return healthProbe.check(url)
                .onErrorReturn(DOWN)
                .map(status -> new SystemStatus.Node(name, url.toString(), status));
    }

    private Mono<SystemStatus.Service> serviceStatus(String serviceId) {
        return Mono.fromCallable(() -> discoveryClient.getInstances(serviceId))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorReturn(List.of())
                .flatMapMany(Flux::fromIterable)
                .flatMap(this::instanceStatus)
                .collectList()
                .map(instances -> new SystemStatus.Service(
                        serviceId,
                        instances.stream().anyMatch(i -> UP.equals(i.status())) ? UP : DOWN,
                        instances));
    }

    private Mono<SystemStatus.Instance> instanceStatus(ServiceInstance instance) {
        URI uri = instance.getUri();
        return healthProbe.check(uri)
                .onErrorReturn(DOWN)
                .map(status -> new SystemStatus.Instance(instance.getInstanceId(), uri.toString(), status));
    }
}

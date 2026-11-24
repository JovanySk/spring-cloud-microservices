package com.jovany.bakery.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTests {

    @Autowired
    private RouteLocator routeLocator;

    private Map<String, Route> loadRoutes() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();
        return routes.stream().collect(Collectors.toMap(Route::getId, route -> route));
    }

    @Test
    void definesOneRoutePerBusinessService() {
        assertThat(loadRoutes()).containsOnlyKeys("catalog-service", "inventory-service", "order-service");
    }

    @Test
    void routesUseServiceDiscoveryInsteadOfHardcodedUrls() {
        Map<String, Route> routes = loadRoutes();

        assertThat(routes.get("catalog-service").getUri()).isEqualTo(URI.create("lb://catalog-service"));
        assertThat(routes.get("inventory-service").getUri()).isEqualTo(URI.create("lb://inventory-service"));
        assertThat(routes.get("order-service").getUri()).isEqualTo(URI.create("lb://order-service"));
    }

    @Test
    void everyRouteHasAStripPrefixFilter() {
        loadRoutes().values().forEach(route ->
                assertThat(route.getFilters())
                        .as("filters of %s", route.getId())
                        .hasSize(1));
    }
}

package com.jovany.bakery.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * HTTP client for catalog-service. There is no URL here: "catalog-service" is the
 * name registered in Eureka and Spring Cloud LoadBalancer picks a live instance.
 */
@FeignClient(name = "catalog-service")
public interface CatalogClient {

    @GetMapping("/products/sku/{sku}")
    ProductDto getProductBySku(@PathVariable("sku") String sku);
}

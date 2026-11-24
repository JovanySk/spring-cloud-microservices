package com.jovany.bakery.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** HTTP client for inventory-service, resolved by name through Eureka. */
@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @PostMapping("/inventory/{sku}/reserve")
    ReservationDto reserve(@PathVariable("sku") String sku, @RequestBody ReserveStockRequest request);
}

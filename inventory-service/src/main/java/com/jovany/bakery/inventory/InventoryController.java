package com.jovany.bakery.inventory;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{sku}")
    public StockResponse getStock(@PathVariable String sku) {
        return inventoryService.getStock(sku);
    }

    /** Used by order-service. Answers 409 Conflict when there is not enough stock. */
    @PostMapping("/{sku}/reserve")
    public ReservationResponse reserve(@PathVariable String sku, @Valid @RequestBody ReserveRequest request) {
        return inventoryService.reserve(sku, request.quantity());
    }
}

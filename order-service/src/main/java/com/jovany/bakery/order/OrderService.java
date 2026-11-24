package com.jovany.bakery.order;

import com.jovany.bakery.order.client.CatalogClient;
import com.jovany.bakery.order.client.InventoryClient;
import com.jovany.bakery.order.client.ProductDto;
import com.jovany.bakery.order.client.ReserveStockRequest;
import feign.FeignException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Places orders by talking to the other two services:
 * 1. asks catalog-service for the current price of every product,
 * 2. asks inventory-service to reserve the units,
 * 3. stores the order in its own database.
 *
 * Remote errors are translated into this service's own exceptions
 * (404 unknown product, 409 not enough stock, 503 dependency down).
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CatalogClient catalogClient;
    private final InventoryClient inventoryClient;

    public OrderService(OrderRepository orderRepository, CatalogClient catalogClient,
                        InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
    }

    // No @Transactional here on purpose: we do not keep a DB connection open while
    // waiting for remote HTTP calls. repository.save() runs in its own transaction.
    public OrderResponse placeOrder(CreateOrderRequest request) {
        // First read every price, so an unknown SKU fails before any stock is touched
        List<OrderLine> lines = new ArrayList<>();
        for (OrderItemRequest item : request.items()) {
            ProductDto product = fetchProduct(item.sku());
            lines.add(new OrderLine(product.sku(), product.name(), item.quantity(), product.price()));
        }

        for (OrderItemRequest item : request.items()) {
            reserveStock(item.sku(), item.quantity());
        }

        Order saved = orderRepository.save(new Order(lines));
        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private ProductDto fetchProduct(String sku) {
        try {
            return catalogClient.getProductBySku(sku);
        } catch (FeignException.NotFound ex) {
            throw new ProductNotFoundException(sku);
        } catch (FeignException ex) {
            // connection refused, timeout, no instance in Eureka, 5xx...
            throw new ServiceUnavailableException("catalog-service", ex);
        }
    }

    private void reserveStock(String sku, int quantity) {
        try {
            inventoryClient.reserve(sku, new ReserveStockRequest(quantity));
        } catch (FeignException.Conflict | FeignException.NotFound ex) {
            // 409 = not enough units, 404 = the product has no stock row at all
            throw new InsufficientStockException(sku, quantity);
        } catch (FeignException ex) {
            throw new ServiceUnavailableException("inventory-service", ex);
        }
    }
}

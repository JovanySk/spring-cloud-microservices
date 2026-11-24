package com.jovany.bakery.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, List<Line> items, BigDecimal total, Instant createdAt) {

    public record Line(String sku, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    static OrderResponse from(Order order) {
        List<Line> items = order.getLines().stream()
                .map(line -> new Line(line.getSku(), line.getProductName(), line.getQuantity(),
                        line.getUnitPrice(), line.lineTotal()))
                .toList();
        return new OrderResponse(order.getId(), items, order.getTotal(), order.getCreatedAt());
    }
}

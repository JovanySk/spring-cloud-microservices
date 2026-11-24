package com.jovany.bakery.catalog;

import java.math.BigDecimal;

/** What the API returns; keeps the JPA entity out of the HTTP contract. */
public record ProductResponse(Long id, String sku, String name, BigDecimal price) {

    static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getSku(), product.getName(), product.getPrice());
    }
}

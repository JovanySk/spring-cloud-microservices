package com.jovany.bakery.order.client;

import java.math.BigDecimal;

/** Product as returned by catalog-service (only the fields this service needs). */
public record ProductDto(Long id, String sku, String name, BigDecimal price) {
}

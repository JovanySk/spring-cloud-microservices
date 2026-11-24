package com.jovany.bakery.inventory;

public record StockResponse(String sku, int quantity, boolean inStock) {

    static StockResponse from(StockItem item) {
        return new StockResponse(item.getSku(), item.getQuantity(), item.getQuantity() > 0);
    }
}

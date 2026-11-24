package com.jovany.bakery.inventory;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(String sku) {
        super("No stock registered for SKU " + sku);
    }
}

package com.jovany.bakery.order;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String sku, int requested) {
        super("Not enough stock for SKU " + sku + " (requested " + requested + ")");
    }
}

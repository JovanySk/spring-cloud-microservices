package com.jovany.bakery.order;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String sku) {
        super("Product with SKU " + sku + " does not exist in the catalog");
    }
}

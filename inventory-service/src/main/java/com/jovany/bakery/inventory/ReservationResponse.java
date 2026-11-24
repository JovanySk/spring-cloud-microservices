package com.jovany.bakery.inventory;

public record ReservationResponse(String sku, int reserved, int remaining) {
}

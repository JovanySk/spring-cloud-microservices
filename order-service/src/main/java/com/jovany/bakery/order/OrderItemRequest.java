package com.jovany.bakery.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(@NotBlank String sku, @NotNull @Min(1) Integer quantity) {
}

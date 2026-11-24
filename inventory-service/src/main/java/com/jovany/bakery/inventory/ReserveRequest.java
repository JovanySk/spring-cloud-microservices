package com.jovany.bakery.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveRequest(@NotNull @Min(1) Integer quantity) {
}

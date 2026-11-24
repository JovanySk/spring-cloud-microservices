package com.jovany.bakery.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against the real H2 database (with the seed data) because the important part,
 * the atomic "decrease only if enough" UPDATE, lives in SQL.
 */
@SpringBootTest
@Transactional // every test is rolled back, so each one starts from the seed data
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Test
    void seedDataIsLoaded() {
        assertThat(inventoryService.getStock("CAK-001").quantity()).isEqualTo(3);
    }

    @Test
    void reserveDecreasesTheStock() {
        ReservationResponse reservation = inventoryService.reserve("PST-001", 5);

        assertThat(reservation.remaining()).isEqualTo(35);
        assertThat(inventoryService.getStock("PST-001").quantity()).isEqualTo(35);
    }

    @Test
    void reservingExactlyTheRemainingUnitsLeavesZero() {
        inventoryService.reserve("CAK-001", 3);

        StockResponse stock = inventoryService.getStock("CAK-001");
        assertThat(stock.quantity()).isZero();
        assertThat(stock.inStock()).isFalse();
    }

    @Test
    void insufficientStockThrowsAndKeepsTheStockUnchanged() {
        assertThatThrownBy(() -> inventoryService.reserve("CAK-001", 4))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("available 3");

        assertThat(inventoryService.getStock("CAK-001").quantity()).isEqualTo(3);
    }

    @Test
    void unknownSkuThrowsNotFound() {
        assertThatThrownBy(() -> inventoryService.reserve("XXX-999", 1))
                .isInstanceOf(StockNotFoundException.class);
    }
}

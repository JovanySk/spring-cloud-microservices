package com.jovany.bakery.inventory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Inserts demo stock on startup when the table is empty (SKUs match the catalog seed). */
@Component
public class InventoryDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(InventoryDataSeeder.class);

    private final StockItemRepository repository;

    public InventoryDataSeeder(StockItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new StockItem("BRD-001", 20),
                new StockItem("BRD-002", 15),
                new StockItem("PST-001", 40),
                new StockItem("PST-002", 60),
                new StockItem("CAK-001", 3)));
        log.info("Seeded stock for {} products", repository.count());
    }
}

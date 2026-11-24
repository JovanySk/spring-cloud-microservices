package com.jovany.bakery.catalog;

import java.math.BigDecimal;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Inserts a few demo products on startup when the table is empty. */
@Component
public class CatalogDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataSeeder.class);

    private final ProductRepository repository;

    public CatalogDataSeeder(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        repository.saveAll(List.of(
                new Product("BRD-001", "Sourdough Loaf", new BigDecimal("4.50")),
                new Product("BRD-002", "Whole Wheat Bread", new BigDecimal("3.80")),
                new Product("PST-001", "Butter Croissant", new BigDecimal("2.20")),
                new Product("PST-002", "Concha", new BigDecimal("1.50")),
                new Product("CAK-001", "Tres Leches Cake", new BigDecimal("18.00"))));
        log.info("Seeded {} bakery products", repository.count());
    }
}

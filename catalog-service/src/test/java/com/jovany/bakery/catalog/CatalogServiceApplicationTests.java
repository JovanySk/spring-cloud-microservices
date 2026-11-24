package com.jovany.bakery.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Full context with the H2 database: checks wiring, JPA mapping and the seed data. */
@SpringBootTest
class CatalogServiceApplicationTests {

    @Autowired
    private ProductService productService;

    @Test
    void seedsBakeryProductsOnStartup() {
        assertThat(productService.findAll()).hasSize(5);
        assertThat(productService.findBySku("BRD-001").name()).isEqualTo("Sourdough Loaf");
    }

    @Test
    void unknownSkuThrowsNotFound() {
        assertThatThrownBy(() -> productService.findBySku("XXX-999"))
                .isInstanceOf(ProductNotFoundException.class);
    }
}

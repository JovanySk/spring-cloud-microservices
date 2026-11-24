package com.jovany.bakery.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.jovany.bakery.order.client.CatalogClient;
import com.jovany.bakery.order.client.InventoryClient;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Full context: Feign clients are created and an order round-trips through the H2 database. */
@SpringBootTest
class OrderServiceApplicationTests {

    @Autowired
    private CatalogClient catalogClient;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void feignClientsAreCreatedAndOrderIsPersistedWithItsLines() {
        assertThat(catalogClient).isNotNull();
        assertThat(inventoryClient).isNotNull();

        Order saved = orderRepository.save(new Order(List.of(
                new OrderLine("PST-002", "Concha", 4, new BigDecimal("1.50")))));

        OrderResponse loaded = orderService.getOrder(saved.getId());
        assertThat(loaded.items()).hasSize(1);
        assertThat(loaded.total()).isEqualByComparingTo("6.00");
    }
}

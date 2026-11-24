package com.jovany.bakery.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jovany.bakery.order.client.CatalogClient;
import com.jovany.bakery.order.client.InventoryClient;
import com.jovany.bakery.order.client.ProductDto;
import com.jovany.bakery.order.client.ReservationDto;
import com.jovany.bakery.order.client.ReserveStockRequest;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Business logic of order-service with the two HTTP clients mocked (no network, no Spring). */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final Request REQUEST = Request.create(
            Request.HttpMethod.GET, "http://test", Map.of(), null, StandardCharsets.UTF_8, null);

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void repositoryReturnsWhatItSaves() {
        org.mockito.Mockito.lenient()
                .when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static CreateOrderRequest orderOf(OrderItemRequest... items) {
        return new CreateOrderRequest(List.of(items));
    }

    private void productExists(String sku, String name, String price) {
        given(catalogClient.getProductBySku(sku)).willReturn(new ProductDto(1L, sku, name, new BigDecimal(price)));
    }

    @Test
    void placesOrderReservingStockAndSavingIt() {
        productExists("PST-001", "Butter Croissant", "2.20");
        given(inventoryClient.reserve(eq("PST-001"), any())).willReturn(new ReservationDto("PST-001", 3, 37));

        OrderResponse order = orderService.placeOrder(orderOf(new OrderItemRequest("PST-001", 3)));

        assertThat(order.items()).hasSize(1);
        assertThat(order.items().get(0).productName()).isEqualTo("Butter Croissant");
        assertThat(order.total()).isEqualByComparingTo("6.60");
        verify(inventoryClient).reserve("PST-001", new ReserveStockRequest(3));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void computesTotalAcrossSeveralLines() {
        productExists("BRD-001", "Sourdough Loaf", "4.50");
        productExists("PST-002", "Concha", "1.50");
        productExists("CAK-001", "Tres Leches Cake", "18.00");

        OrderResponse order = orderService.placeOrder(orderOf(
                new OrderItemRequest("BRD-001", 2),   //  9.00
                new OrderItemRequest("PST-002", 6),   //  9.00
                new OrderItemRequest("CAK-001", 1))); // 18.00

        assertThat(order.total()).isEqualByComparingTo("36.00");
        assertThat(order.items()).extracting(OrderResponse.Line::lineTotal)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("9.00"), new BigDecimal("9.00"), new BigDecimal("18.00"));
    }

    @Test
    void usesThePriceFromTheCatalogNotFromTheClient() {
        productExists("BRD-002", "Whole Wheat Bread", "3.80");

        OrderResponse order = orderService.placeOrder(orderOf(new OrderItemRequest("BRD-002", 1)));

        assertThat(order.items().get(0).unitPrice()).isEqualByComparingTo("3.80");
    }

    @Test
    void insufficientStockIsReportedAndOrderIsNotSaved() {
        productExists("CAK-001", "Tres Leches Cake", "18.00");
        given(inventoryClient.reserve(eq("CAK-001"), any()))
                .willThrow(new FeignException.Conflict("conflict", REQUEST, null, null));

        assertThatThrownBy(() -> orderService.placeOrder(orderOf(new OrderItemRequest("CAK-001", 99))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("CAK-001");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void unknownSkuFailsBeforeAnyStockIsReserved() {
        productExists("BRD-001", "Sourdough Loaf", "4.50");
        given(catalogClient.getProductBySku("NOPE-1"))
                .willThrow(new FeignException.NotFound("not found", REQUEST, null, null));

        assertThatThrownBy(() -> orderService.placeOrder(orderOf(
                new OrderItemRequest("BRD-001", 1),
                new OrderItemRequest("NOPE-1", 1))))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("NOPE-1");
        verify(inventoryClient, never()).reserve(anyString(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void catalogDownBecomesServiceUnavailable() {
        given(catalogClient.getProductBySku("BRD-001"))
                .willThrow(new RetryableException(-1, "Connection refused", Request.HttpMethod.GET,
                        (Long) null, REQUEST));

        assertThatThrownBy(() -> orderService.placeOrder(orderOf(new OrderItemRequest("BRD-001", 1))))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("catalog-service");
    }

    @Test
    void inventoryDownBecomesServiceUnavailable() {
        productExists("BRD-001", "Sourdough Loaf", "4.50");
        given(inventoryClient.reserve(eq("BRD-001"), any()))
                .willThrow(new FeignException.ServiceUnavailable("no instances", REQUEST, null, null));

        assertThatThrownBy(() -> orderService.placeOrder(orderOf(new OrderItemRequest("BRD-001", 1))))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("inventory-service");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderThrowsWhenItDoesNotExist() {
        given(orderRepository.findById(42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(42L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("Order 42 not found");
    }

    @Test
    void lineTotalsAreRoundedToCents() {
        List<OrderLine> lines = List.of(new OrderLine("X", "Cookie", 3, new BigDecimal("0.333")));

        assertThat(Order.calculateTotal(lines)).isEqualByComparingTo("1.00");
        assertThat(Order.calculateTotal(lines).scale()).isEqualTo(2);
    }
}

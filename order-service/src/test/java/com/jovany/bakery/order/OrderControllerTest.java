package com.jovany.bakery.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String VALID_ORDER = """
            {"items": [{"sku": "PST-001", "quantity": 3}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private final OrderResponse sampleOrder = new OrderResponse(
            7L,
            List.of(new OrderResponse.Line("PST-001", "Butter Croissant", 3,
                    new BigDecimal("2.20"), new BigDecimal("6.60"))),
            new BigDecimal("6.60"),
            Instant.parse("2026-01-15T10:00:00Z"));

    @Test
    void createsOrderAndReturns201WithLocation() throws Exception {
        given(orderService.placeOrder(any())).willReturn(sampleOrder);

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/orders/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.total").value(6.60))
                .andExpect(jsonPath("$.items[0].productName").value("Butter Croissant"));
    }

    @Test
    void returns409WhenStockIsInsufficient() throws Exception {
        given(orderService.placeOrder(any())).willThrow(new InsufficientStockException("PST-001", 3));

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Not enough stock for SKU PST-001 (requested 3)"));
    }

    @Test
    void returns404WhenProductIsUnknown() throws Exception {
        given(orderService.placeOrder(any())).willThrow(new ProductNotFoundException("PST-001"));

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns503WhenADependencyIsDown() throws Exception {
        given(orderService.placeOrder(any()))
                .willThrow(new ServiceUnavailableException("inventory-service", new RuntimeException("refused")));

        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content(VALID_ORDER))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail")
                        .value("inventory-service is not available right now, please try again later"));
    }

    @Test
    void rejectsOrderWithoutItems() throws Exception {
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("{\"items\": []}"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).placeOrder(any());
    }

    @Test
    void rejectsItemWithInvalidQuantity() throws Exception {
        mockMvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"sku\": \"PST-001\", \"quantity\": 0}]}"))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).placeOrder(any());
    }

    @Test
    void returnsOrderById() throws Exception {
        given(orderService.getOrder(7L)).willReturn(sampleOrder);

        mockMvc.perform(get("/orders/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").value("PST-001"));
    }

    @Test
    void returns404ForUnknownOrder() throws Exception {
        given(orderService.getOrder(99L)).willThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/orders/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order 99 not found"));
    }
}

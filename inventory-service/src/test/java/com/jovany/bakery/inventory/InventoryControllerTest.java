package com.jovany.bakery.inventory;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void returnsStockForSku() throws Exception {
        given(inventoryService.getStock("BRD-001")).willReturn(new StockResponse("BRD-001", 20, true));

        mockMvc.perform(get("/inventory/BRD-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("BRD-001"))
                .andExpect(jsonPath("$.quantity").value(20))
                .andExpect(jsonPath("$.inStock").value(true));
    }

    @Test
    void returns404ForUnknownSku() throws Exception {
        given(inventoryService.getStock("NOPE")).willThrow(new StockNotFoundException("NOPE"));

        mockMvc.perform(get("/inventory/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No stock registered for SKU NOPE"));
    }

    @Test
    void reservesStock() throws Exception {
        given(inventoryService.reserve("PST-001", 5)).willReturn(new ReservationResponse("PST-001", 5, 35));

        mockMvc.perform(post("/inventory/PST-001/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reserved").value(5))
                .andExpect(jsonPath("$.remaining").value(35));
    }

    @Test
    void returns409WhenStockIsInsufficient() throws Exception {
        given(inventoryService.reserve("CAK-001", 10)).willThrow(new InsufficientStockException("CAK-001", 10, 3));

        mockMvc.perform(post("/inventory/CAK-001/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Insufficient stock for SKU CAK-001: requested 10, available 3"));
    }

    @Test
    void rejectsNonPositiveQuantity() throws Exception {
        mockMvc.perform(post("/inventory/CAK-001/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 0}"))
                .andExpect(status().isBadRequest());

        verify(inventoryService, never()).reserve(anyString(), anyInt());
    }
}

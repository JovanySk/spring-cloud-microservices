package com.jovany.bakery.catalog;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private final ProductResponse croissant =
            new ProductResponse(1L, "PST-001", "Butter Croissant", new BigDecimal("2.20"));

    @Test
    void listsAllProducts() throws Exception {
        given(productService.findAll()).willReturn(List.of(
                croissant,
                new ProductResponse(2L, "BRD-001", "Sourdough Loaf", new BigDecimal("4.50"))));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sku").value("PST-001"))
                .andExpect(jsonPath("$[1].price").value(4.50));
    }

    @Test
    void returnsProductById() throws Exception {
        given(productService.findById(1L)).willReturn(croissant);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Butter Croissant"));
    }

    @Test
    void returnsProductBySku() throws Exception {
        given(productService.findBySku("PST-001")).willReturn(croissant);

        mockMvc.perform(get("/products/sku/PST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.price").value(2.20));
    }

    @Test
    void returns404WhenSkuDoesNotExist() throws Exception {
        given(productService.findBySku("NOPE")).willThrow(new ProductNotFoundException("Product with SKU NOPE not found"));

        mockMvc.perform(get("/products/sku/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product with SKU NOPE not found"));
    }
}

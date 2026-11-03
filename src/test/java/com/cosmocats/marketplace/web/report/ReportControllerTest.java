package com.cosmocats.marketplace.web.report;

import com.cosmocats.marketplace.application.report.PopularProduct;
import com.cosmocats.marketplace.application.report.ProductReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@DisplayName("ReportController")
class ReportControllerTest {

    private static final String POPULAR_PRODUCTS_URL = "/api/v1/reports/popular-products";
    private static final UUID MILK_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductReportService productReportService;

    @Test
    void returnsTenMostPopularProductsByDefault() throws Exception {
        when(productReportService.getPopularProducts(10))
                .thenReturn(List.of(new PopularProduct(MILK_ID, "Galaxy cosmic milk", 42, 17)));

        mockMvc.perform(get(POPULAR_PRODUCTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(MILK_ID.toString()))
                .andExpect(jsonPath("$[0].productName").value("Galaxy cosmic milk"))
                .andExpect(jsonPath("$[0].totalQuantity").value(42))
                .andExpect(jsonPath("$[0].orderCount").value(17));
    }

    @Test
    void passesRequestedLimit() throws Exception {
        mockMvc.perform(get(POPULAR_PRODUCTS_URL).param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(productReportService).getPopularProducts(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101", "-1"})
    void rejectsLimitOutOfRange(String limit) throws Exception {
        mockMvc.perform(get(POPULAR_PRODUCTS_URL).param("limit", limit))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("limit"));
        verifyNoInteractions(productReportService);
    }
}

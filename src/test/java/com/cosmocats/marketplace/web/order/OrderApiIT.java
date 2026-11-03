package com.cosmocats.marketplace.web.order;

import com.cosmocats.marketplace.AbstractIT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Orders end-to-end against PostgreSQL. Every test creates its own products in the Cosmic food
 * category, so the stock and report numbers do not depend on other tests.
 */
@DisplayName("Order API end-to-end")
class OrderApiIT extends AbstractIT {

    private static final String COSMIC_FOOD_ID = "22222222-2222-2222-2222-222222222222";
    private static final String LUNA_ID = "cccccccc-0000-0000-0000-000000000001";
    private static final String ERRORS_BASE_URL = "https://cosmo-cats.market/errors/";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void placesOrderAndTakesItemsFromStock() throws Exception {
        String milk = createProduct("Order test galaxy milk", "4.99", 5);
        String cartId = UUID.randomUUID().toString();

        placeOrder(cartId, LUNA_ID, milk, 2)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/orders/" + cartId))
                .andExpect(jsonPath("$.items[0].unitPrice").value(4.99))
                .andExpect(jsonPath("$.totalPrice").value(9.98));

        mockMvc.perform(get("/api/v1/orders/{cartId}", cartId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(LUNA_ID))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items[0].productId").value(milk))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
        assertThat(stockOf(milk)).isEqualTo(3);
    }

    @Test
    void orderKeepsPriceOfTheMomentOfOrdering() throws Exception {
        String milk = createProduct("Order test price galaxy milk", "4.99", 5);
        String cartId = UUID.randomUUID().toString();
        placeOrder(cartId, LUNA_ID, milk, 1).andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/products/{id}", milk)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Order test price galaxy milk", "9.99", 4)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/{cartId}", cartId))
                .andExpect(jsonPath("$.items[0].unitPrice").value(4.99));
    }

    @Test
    void sameCartCannotBeCheckedOutTwice() throws Exception {
        String milk = createProduct("Order test twice galaxy milk", "4.99", 5);
        String cartId = UUID.randomUUID().toString();
        placeOrder(cartId, LUNA_ID, milk, 1).andExpect(status().isCreated());

        placeOrder(cartId, LUNA_ID, milk, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "order-already-exists"));
        assertThat(stockOf(milk)).isEqualTo(4);
    }

    @Test
    void failedItemRollsBackStockOfItemsBeforeIt() throws Exception {
        String milk = createProduct("Order test rollback galaxy milk", "4.99", 5);
        String capsule = createProduct("Order test rollback nebula capsule", "89.90", 1);
        String cartId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                        {"cartId": "%s", "customerId": "%s", "items": [
                          {"productId": "%s", "quantity": 2},
                          {"productId": "%s", "quantity": 2}]}
                        """.formatted(cartId, LUNA_ID, milk, capsule)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "insufficient-stock"));

        // The milk was taken from stock before the capsule failed, the transaction put it back
        assertThat(stockOf(milk)).isEqualTo(5);
        assertThat(stockOf(capsule)).isEqualTo(1);
        mockMvc.perform(get("/api/v1/orders/{cartId}", cartId)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnknownCosmoCatAndUnknownProduct() throws Exception {
        String milk = createProduct("Order test unknown galaxy milk", "4.99", 5);

        placeOrder(UUID.randomUUID().toString(), UUID.randomUUID().toString(), milk, 1)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "order-reference-not-found"));
        placeOrder(UUID.randomUUID().toString(), LUNA_ID, UUID.randomUUID().toString(), 1)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "order-reference-not-found"));
        assertThat(stockOf(milk)).isEqualTo(5);
    }

    @Test
    void orderedProductCannotBeDeleted() throws Exception {
        String milk = createProduct("Order test ordered galaxy milk", "4.99", 5);
        placeOrder(UUID.randomUUID().toString(), LUNA_ID, milk, 1).andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/products/{id}", milk))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "product-in-use"));
        mockMvc.perform(get("/api/v1/products/{id}", milk)).andExpect(status().isOk());
    }

    @Test
    void reportShowsMostOrderedProductsFirst() throws Exception {
        String milk = createProduct("Report test galaxy milk", "4.99", 100);
        String capsule = createProduct("Report test nebula capsule", "89.90", 100);
        placeOrder(UUID.randomUUID().toString(), LUNA_ID, milk, 3).andExpect(status().isCreated());
        placeOrder(UUID.randomUUID().toString(), LUNA_ID, capsule, 1).andExpect(status().isCreated());
        placeOrder(UUID.randomUUID().toString(), "cccccccc-0000-0000-0000-000000000002", milk, 2)
                .andExpect(status().isCreated());

        JsonNode report = objectMapper.readTree(mockMvc.perform(get("/api/v1/reports/popular-products")
                        .param("limit", "100"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());

        int milkPosition = -1;
        int capsulePosition = -1;
        for (int i = 0; i < report.size(); i++) {
            JsonNode row = report.get(i);
            if (row.get("productId").asText().equals(milk)) {
                milkPosition = i;
                assertThat(row.get("productName").asText()).isEqualTo("Report test galaxy milk");
                assertThat(row.get("totalQuantity").asLong()).isEqualTo(5);
                assertThat(row.get("orderCount").asLong()).isEqualTo(2);
            }
            if (row.get("productId").asText().equals(capsule)) {
                capsulePosition = i;
                assertThat(row.get("totalQuantity").asLong()).isEqualTo(1);
            }
        }
        assertThat(milkPosition).isNotNegative().isLessThan(capsulePosition);
    }

    private String createProduct(String name, String price, int stock) throws Exception {
        String response = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(name, price, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private int stockOf(String productId) throws Exception {
        String response = mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("stockQuantity").asInt();
    }

    private ResultActions placeOrder(String cartId, String customerId, String productId, int quantity) throws Exception {
        return mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"cartId": "%s", "customerId": "%s", "items": [{"productId": "%s", "quantity": %d}]}
                """.formatted(cartId, customerId, productId, quantity)));
    }

    private static String productJson(String name, String price, int stock) {
        return """
                {"name": "%s", "price": %s, "stockQuantity": %d, "categoryId": "%s"}
                """.formatted(name, price, stock, COSMIC_FOOD_ID);
    }
}

package com.cosmocats.marketplace.web.product;

import com.cosmocats.marketplace.AbstractIT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs whole scenarios through all layers: controller, validation, services and in-memory repositories.
 */
@DisplayName("Product API end-to-end")
class ProductApiIT extends AbstractIT {

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String COSMIC_FOOD_ID = "22222222-2222-2222-2222-222222222222";
    private static final String SPACE_GEAR_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createReadUpdateDeleteProduct() throws Exception {
        MvcResult created = mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Moon dust catnip", "7.25", 50, COSMIC_FOOD_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String location = created.getResponse().getHeader("Location");
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        assertThat(location).isEqualTo("http://localhost/api/v1/products/" + id);

        mockMvc.perform(get(PRODUCTS_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Moon dust catnip"))
                .andExpect(jsonPath("$.price").value(7.25));

        mockMvc.perform(put(PRODUCTS_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Moon dust catnip XL", "9.90", 10, SPACE_GEAR_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Moon dust catnip XL"))
                .andExpect(jsonPath("$.categoryId").value(SPACE_GEAR_ID));

        mockMvc.perform(delete(PRODUCTS_URL + "/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get(PRODUCTS_URL + "/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(delete(PRODUCTS_URL + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicateNameIgnoringCaseAndSurroundingSpaces() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("  GALAXY cosmic MILK ", "1.00", 1, COSMIC_FOOD_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/product-already-exists"));
    }

    @Test
    void rejectsUnknownCategory() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Comet sparkle collar", "5.00", 1, "99999999-9999-9999-9999-999999999999")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/category-not-found"));
    }

    @Test
    void walksThroughAllPagesWithTokens() throws Exception {
        List<String> names = new ArrayList<>();
        String pageToken = null;
        int requests = 0;
        do {
            var request = get(PRODUCTS_URL).param("pageSize", "1");
            if (pageToken != null) {
                request.param("pageToken", pageToken);
            }
            JsonNode page = objectMapper.readTree(mockMvc.perform(request)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString());
            page.get("content").forEach(product -> names.add(product.get("name").asText()));
            pageToken = page.get("page").get("nextPageToken").isNull() ? null : page.get("page").get("nextPageToken").asText();
            requests++;
        } while (pageToken != null && requests < 20);

        assertThat(names)
                .contains("Anti-gravity star yarn ball", "Comet tail scratching post",
                        "Galaxy cosmic milk", "Nebula nap capsule")
                .doesNotHaveDuplicates()
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    void servesOpenApiContract() throws Exception {
        mockMvc.perform(get("/api-specs/cosmo-cats-product-api.yml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openapi: 3.0.3")));
    }

    private String productJson(String name, String price, int stockQuantity, String categoryId) {
        return """
                {"name": "%s", "price": %s, "stockQuantity": %d, "categoryId": "%s"}
                """.formatted(name, price, stockQuantity, categoryId);
    }
}

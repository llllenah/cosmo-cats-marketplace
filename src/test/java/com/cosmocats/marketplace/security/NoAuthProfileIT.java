package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.AbstractIT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole application with the no-auth profile and no user at all. Besides the filter chain
 * this also shows that @PreAuthorize on the services is off: with it an anonymous caller
 * would get 403 for the admin-only operations below.
 */
@ActiveProfiles("no-auth")
@WithAnonymousUser
@DisplayName("Profile no-auth end-to-end")
class NoAuthProfileIT extends AbstractIT {

    private static final String SPACE_GEAR_ID = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void anonymousCallerDoesEverythingAnAdminCan() throws Exception {
        String created = mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "No auth test galaxy helmet", "price": 19.99, "stockQuantity": 2, "categoryId": "%s"}
                        """.formatted(SPACE_GEAR_ID)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String productId = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/v1/products/{id}", productId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reports/popular-products")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/products/{id}", productId)).andExpect(status().isNoContent());
    }

    @Test
    void accountPageDoesNotExistBecauseNobodyLogsIn() throws Exception {
        mockMvc.perform(get("/account")).andExpect(status().isNotFound());
    }
}

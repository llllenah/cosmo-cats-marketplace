package com.cosmocats.marketplace.web.cat;

import com.cosmocats.marketplace.AbstractIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The same application in an environment where the flags are switched the other way,
 * without any code change.
 */
@DisplayName("Feature toggles end-to-end, flags overridden for the environment")
@TestPropertySource(properties = {"feature.cosmoCats.enabled=false", "feature.kittyProducts.enabled=true"})
class SwitchedFeatureTogglesIT extends AbstractIT {

    @Test
    void cosmoCatsAreNotAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/cosmo-cats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Feature 'cosmoCats' is not available."));
    }

    @Test
    void kittyProductsAreToysFromConfiguredCategory() throws Exception {
        mockMvc.perform(get("/api/v1/kitty-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Anti-gravity star yarn ball")))
                .andExpect(jsonPath("$[0].categoryId").value("11111111-1111-1111-1111-111111111111"));
    }
}

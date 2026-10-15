package com.cosmocats.marketplace.web.cat;

import com.cosmocats.marketplace.AbstractIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Feature toggles in the running application with the flags from application.yml:
 * cosmoCats is on, kittyProducts is off.
 */
@DisplayName("Feature toggles end-to-end, flags from application.yml")
class DefaultFeatureTogglesIT extends AbstractIT {

    @Test
    void cosmoCatsAreAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/cosmo-cats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("Luna", "Nova", "Orion")));
    }

    @Test
    void kittyProductsAreNotAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/kitty-products"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/feature-not-available"));
    }

    @Test
    void endpointsWithoutToggleAreNotAffected() throws Exception {
        mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());
    }
}

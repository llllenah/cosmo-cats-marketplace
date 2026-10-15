package com.cosmocats.marketplace.web.cat;

import com.cosmocats.marketplace.application.cat.CosmoCatService;
import com.cosmocats.marketplace.application.product.KittyProductService;
import com.cosmocats.marketplace.domain.cat.CosmoCat;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.featuretoggle.Feature;
import com.cosmocats.marketplace.featuretoggle.FeatureNotAvailableException;
import com.cosmocats.marketplace.web.common.PageTokenCodec;
import com.cosmocats.marketplace.web.product.KittyProductController;
import com.cosmocats.marketplace.web.product.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({CosmoCatController.class, KittyProductController.class})
@Import({CosmoCatMapper.class, ProductMapper.class, PageTokenCodec.class})
@DisplayName("Controllers behind feature toggles")
class FeatureToggledControllersTest {

    private static final UUID LUNA_ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CosmoCatService cosmoCatService;

    @MockBean
    private KittyProductService kittyProductService;

    @Test
    void returnsCosmoCats() throws Exception {
        when(cosmoCatService.getCosmoCats()).thenReturn(List.of(new CosmoCat(LUNA_ID, "Luna", "Moon")));

        mockMvc.perform(get("/api/v1/cosmo-cats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(LUNA_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("Luna"))
                .andExpect(jsonPath("$[0].homePlanet").value("Moon"));
    }

    @Test
    void returns404ProblemWhenCosmoCatsFeatureIsDisabled() throws Exception {
        when(cosmoCatService.getCosmoCats()).thenThrow(new FeatureNotAvailableException(Feature.COSMO_CATS));

        mockMvc.perform(get("/api/v1/cosmo-cats"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/feature-not-available"))
                .andExpect(jsonPath("$.title").value("Feature not available"))
                .andExpect(jsonPath("$.detail").value("Feature 'cosmoCats' is not available."))
                .andExpect(jsonPath("$.instance").value("/api/v1/cosmo-cats"));
    }

    @Test
    void returnsKittyProducts() throws Exception {
        UUID productId = UUID.randomUUID();
        when(kittyProductService.getKittyProducts()).thenReturn(List.of(new Product(productId,
                "Anti-gravity star yarn ball", null, new BigDecimal("12.50"), 120, UUID.randomUUID())));

        mockMvc.perform(get("/api/v1/kitty-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(productId.toString()))
                .andExpect(jsonPath("$[0].price").value(12.50));
    }

    @Test
    void returns404ProblemWhenKittyProductsFeatureIsDisabled() throws Exception {
        when(kittyProductService.getKittyProducts()).thenThrow(new FeatureNotAvailableException(Feature.KITTY_PRODUCTS));

        mockMvc.perform(get("/api/v1/kitty-products"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Feature 'kittyProducts' is not available."));
    }
}

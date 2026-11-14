package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.application.pricing.ProductPriceService;
import com.cosmocats.marketplace.application.product.ProductService;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.web.common.PageTokenCodec;
import com.cosmocats.marketplace.web.product.ProductController;
import com.cosmocats.marketplace.web.product.ProductMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Both security configurations are imported like in the real application,
 * the profile decides which one of them exists.
 */
@WebMvcTest(ProductController.class)
@Import({ProductMapper.class, PageTokenCodec.class, SecurityConfig.class, NoAuthSecurityConfig.class})
@ActiveProfiles("no-auth")
@DisplayName("Profile no-auth")
class NoAuthProfileTest {

    private static final UUID PRODUCT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductPriceService productPriceService;

    @Test
    void requestsWithoutCredentialsAreAllowed() throws Exception {
        when(productService.getProductById(PRODUCT_ID)).thenReturn(
                new Product(PRODUCT_ID, "Galaxy cosmic milk", "Milk", new BigDecimal("4.99"), 3, CATEGORY_ID));

        mockMvc.perform(get("/api/v1/products/{id}", PRODUCT_ID))
                .andExpect(status().isOk());
    }

    @Test
    void writeRequestsNeedNeitherCredentialsNorCsrfToken() throws Exception {
        when(productService.createProduct(any())).thenAnswer(invocation ->
                ((Product) invocation.getArgument(0)).withId(PRODUCT_ID));

        mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("""
                        {"name": "Galaxy test ball", "price": 5.00, "stockQuantity": 1,
                         "categoryId": "22222222-2222-2222-2222-222222222222"}
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void pagesAreNotSentToGitHubLogin() throws Exception {
        // No controller for this path in the test, the point is that it is 404 and not a redirect to GitHub
        mockMvc.perform(get("/account"))
                .andExpect(status().isNotFound());
    }

    @Test
    void realSecurityConfigurationIsNotLoaded() {
        assertThat(applicationContext.getBeansOfType(SecurityFilterChain.class)).containsOnlyKeys("noAuthFilterChain");
        assertThat(applicationContext.getBeansOfType(SecurityConfig.class)).isEmpty();
    }

    @Test
    void refusesToStartTogetherWithProductionProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("no-auth", "prod");

        assertThatThrownBy(() -> new NoAuthSecurityConfig().noAuthFilterChain(null, environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be combined with a production profile");
    }
}

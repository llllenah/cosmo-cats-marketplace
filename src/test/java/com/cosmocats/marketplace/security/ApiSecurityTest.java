package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.application.pricing.ProductPriceService;
import com.cosmocats.marketplace.application.product.PagedResult;
import com.cosmocats.marketplace.application.product.ProductService;
import com.cosmocats.marketplace.application.report.ProductReportService;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.security.apikey.ApiKeyAuthenticationToken;
import com.cosmocats.marketplace.web.common.PageTokenCodec;
import com.cosmocats.marketplace.web.product.ProductController;
import com.cosmocats.marketplace.web.product.ProductMapper;
import com.cosmocats.marketplace.web.report.ReportController;
import com.cosmocats.marketplace.web.report.ReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who gets through the /api filter chain. The filters, the API key provider and the conversion
 * of a token into roles are real; only the token signature check (JwtDecoder) is replaced,
 * the real one is covered by SecurityIT with signed tokens and a JWKS endpoint.
 * There is no @WithMockUser on purpose: every request has to authenticate by itself.
 */
@WebMvcTest({ProductController.class, ReportController.class})
@Import({ProductMapper.class, PageTokenCodec.class, ReportMapper.class, SecurityConfig.class})
@TestPropertySource(properties = {
        // SHA-256 of "committee-test-key"
        "application.security.api-keys.intergalactic-committee.key-sha256="
                + "9c3ab4daf77d5eb2aa8db948f6fc5ae61db834d32b0029c9241728a7c6caf953",
        "application.security.api-keys.intergalactic-committee.roles=COMMITTEE",
        "application.security.api-keys.client-without-key.key-sha256=",
        "application.security.api-keys.client-without-key.roles=ADMIN"
})
@DisplayName("Security of /api")
class ApiSecurityTest {

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String REPORT_URL = "/api/v1/reports/popular-products";
    private static final String ERRORS_BASE_URL = "https://cosmo-cats.market/errors/";
    private static final String COMMITTEE_KEY = "committee-test-key";
    private static final String VALID_TOKEN = "valid-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ProductService productService;

    @MockBean
    private ProductPriceService productPriceService;

    @MockBean
    private ProductReportService productReportService;

    // Who the controller saw as the caller
    private final AtomicReference<Authentication> caller = new AtomicReference<>();

    @BeforeEach
    void rememberCaller() {
        when(productService.getProducts(any(), anyInt())).thenAnswer(invocation -> {
            caller.set(SecurityContextHolder.getContext().getAuthentication());
            return new PagedResult<Product>(List.of(), 20, 0, null);
        });
        when(productReportService.getPopularProducts(anyInt())).thenAnswer(invocation -> {
            caller.set(SecurityContextHolder.getContext().getAuthentication());
            return List.of();
        });
    }

    @Test
    void requestWithoutCredentialsGets401Problem() throws Exception {
        expectUnauthorized(mockMvc.perform(get(PRODUCTS_URL)))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.title").value("Authentication required"))
                .andExpect(jsonPath("$.detail").value("A valid bearer token or API key is required."))
                .andExpect(jsonPath("$.instance").value(PRODUCTS_URL));
        verifyNoInteractions(productService);
    }

    @Test
    void bearerTokenIsAcceptedAndRolesComeFromRolesClaim() throws Exception {
        when(jwtDecoder.decode(VALID_TOKEN)).thenReturn(token()
                .claim("roles", List.of("ADMIN", "COSMO_CAT"))
                .build());

        mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk());

        assertThat(caller.get()).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(caller.get().getName()).isEqualTo("luna@moon.space");
        assertThat(caller.get().getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_COSMO_CAT");
    }

    @Test
    void scopesOfTokenDoNotBecomeRoles() throws Exception {
        // A client must not make itself an admin by asking the provider for a scope called ADMIN
        when(jwtDecoder.decode(VALID_TOKEN)).thenReturn(token().claim("scope", "ADMIN ROLE_ADMIN").build());

        mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isOk());

        assertThat(caller.get().getAuthorities()).isEmpty();
    }

    @Test
    void rejectedTokenGets401WithReasonInHeaderOnly() throws Exception {
        when(jwtDecoder.decode("expired-token")).thenThrow(new BadJwtException("Jwt expired at 2026-11-14T10:00:00Z"));

        expectUnauthorized(mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer expired-token")))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer error=\"invalid_token\"")))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Jwt expired")))
                .andExpect(jsonPath("$.detail").value("A valid bearer token or API key is required."));
        verifyNoInteractions(productService);
    }

    @Test
    void identityProviderOutageIsNotReportedAsClientError() throws Exception {
        // NimbusJwtDecoder throws a plain JwtException when it cannot download the keys:
        // the token may be perfectly fine, the application just cannot check it right now
        when(jwtDecoder.decode(VALID_TOKEN)).thenThrow(
                new org.springframework.security.oauth2.jwt.JwtException("Couldn't retrieve remote JWK set"));

        mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "authentication-unavailable"))
                .andExpect(jsonPath("$.instance").value(PRODUCTS_URL));
        verifyNoInteractions(productService);
    }

    @Test
    void postWithTokenDoesNotNeedCsrfToken() throws Exception {
        when(jwtDecoder.decode(VALID_TOKEN)).thenReturn(token().build());
        when(productService.createProduct(any())).thenAnswer(invocation ->
                ((Product) invocation.getArgument(0)).withId(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000009")));

        mockMvc.perform(post(PRODUCTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Galaxy test ball", "price": 5.00, "stockQuantity": 1,
                                 "categoryId": "22222222-2222-2222-2222-222222222222"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void apiKeyLogsInAsConfiguredClientWithItsRoles() throws Exception {
        mockMvc.perform(get(REPORT_URL).header("X-API-Key", COMMITTEE_KEY))
                .andExpect(status().isOk());

        assertThat(caller.get()).isInstanceOf(ApiKeyAuthenticationToken.class);
        assertThat(caller.get().getName()).isEqualTo("intergalactic-committee");
        assertThat(caller.get().getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_COMMITTEE");
        // The key itself does not travel further than the filter
        assertThat(caller.get().getCredentials()).isNull();
        verifyNoInteractions(jwtDecoder);
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
            "wrong-key",
            "Committee-Test-Key",
            "committee-test-key ",
            "",
            "   ",
            // SHA-256 of the real key: the hash from the configuration must not work as a key
            "9c3ab4daf77d5eb2aa8db948f6fc5ae61db834d32b0029c9241728a7c6caf953"
    })
    void invalidApiKeyGets401(String apiKey) throws Exception {
        expectUnauthorized(mockMvc.perform(get(REPORT_URL).header("X-API-Key", apiKey)));
        verifyNoInteractions(productReportService);
    }

    @Test
    void veryLongApiKeyGets401() throws Exception {
        expectUnauthorized(mockMvc.perform(get(REPORT_URL).header("X-API-Key", "k".repeat(10_000))));
    }

    @Test
    void apiKeyTogetherWithTokenGets401() throws Exception {
        when(jwtDecoder.decode(VALID_TOKEN)).thenReturn(token().claim("roles", List.of("ADMIN")).build());

        expectUnauthorized(mockMvc.perform(get(REPORT_URL)
                .header("X-API-Key", COMMITTEE_KEY)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)));
        verifyNoInteractions(productReportService, jwtDecoder);
    }

    private static ResultActions expectUnauthorized(ResultActions result) throws Exception {
        return result
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "unauthorized"))
                .andExpect(jsonPath("$.status").value(401));
    }

    private static Jwt.Builder token() {
        return Jwt.withTokenValue(VALID_TOKEN)
                .header("alg", "RS256")
                .subject("luna@moon.space")
                .claim("email", "luna@moon.space");
    }
}

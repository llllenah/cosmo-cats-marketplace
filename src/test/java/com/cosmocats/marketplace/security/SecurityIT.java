package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.AbstractIT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security end-to-end, nothing is mocked: tokens are signed with the development key, the application
 * downloads the JWKS file from WireMock (the same file the Docker container serves) and checks
 * signature, issuer, audience and expiry itself. The mock admin of AbstractIT is switched off,
 * every request authenticates with what it sends.
 */
@WithAnonymousUser
@TestPropertySource(properties =
        // SHA-256 of "committee-test-key"
        "application.security.api-keys.intergalactic-committee.key-sha256="
                + "9c3ab4daf77d5eb2aa8db948f6fc5ae61db834d32b0029c9241728a7c6caf953")
@DisplayName("Security end-to-end")
class SecurityIT extends AbstractIT {

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String REPORT_URL = "/api/v1/reports/popular-products";
    private static final String ERRORS_BASE_URL = "https://cosmo-cats.market/errors/";
    private static final String COSMIC_FOOD_ID = "22222222-2222-2222-2222-222222222222";
    private static final String LUNA_ID = "cccccccc-0000-0000-0000-000000000001";
    private static final String ORION_ID = "cccccccc-0000-0000-0000-000000000002";
    private static final String LUNA_EMAIL = "luna@moon.space";
    private static final String ORION_EMAIL = "orion@mars.space";
    private static final String COMMITTEE_KEY = "committee-test-key";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requestWithoutCredentialsIsRejected() throws Exception {
        expectUnauthorized(mockMvc.perform(get(PRODUCTS_URL)));
    }

    // The keys come from the JWKS file served by WireMock. The decoder caches them, so the request
    // log of WireMock (cleared before every test) cannot show the download; IdentityProviderDownIT
    // proves it instead: without the JWKS endpoint the same token is not accepted
    @Test
    void tokenSignedWithKeyOfIdentityProviderIsAccepted() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).with(token("COSMO_CAT", LUNA_EMAIL)))
                .andExpect(status().isOk());
    }

    @Test
    void adminManagesCatalogAndCosmoCatOnlyReadsIt() throws Exception {
        String productId = createProduct("Security test galaxy treat", 5);

        mockMvc.perform(get(PRODUCTS_URL + "/{id}", productId).with(token("COSMO_CAT", LUNA_EMAIL)))
                .andExpect(status().isOk());
        expectForbidden(mockMvc.perform(post(PRODUCTS_URL).with(token("COSMO_CAT", LUNA_EMAIL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Security test comet treat", 5))));
        expectForbidden(mockMvc.perform(delete(PRODUCTS_URL + "/{id}", productId).with(token("COSMO_CAT", LUNA_EMAIL))));

        // Still there after the denied delete, and the admin can remove it
        mockMvc.perform(delete(PRODUCTS_URL + "/{id}", productId).with(token("ADMIN", "admin@cosmo-cats.market")))
                .andExpect(status().isNoContent());
    }

    static Stream<Arguments> tokensThatMustBeRejected() throws Exception {
        Instant now = Instant.now();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair attackerKey = generator.generateKeyPair();
        JWTClaimsSet adminClaims = adminClaims().build();

        SignedJWT signedWithSharedSecret = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID(DevTokens.KEY_ID).build(), adminClaims);
        signedWithSharedSecret.sign(new MACSigner("a-secret-the-attacker-made-up-himself"));

        String validToken = DevTokens.sign(adminClaims, DevTokens.devPrivateKey());
        String[] parts = validToken.split("\\.");
        String payloadOfCat = DevTokens.sign(DevTokens.claims(List.of("COSMO_CAT"), LUNA_EMAIL).build(),
                DevTokens.devPrivateKey()).split("\\.")[1];

        return Stream.of(
                Arguments.of("expired", DevTokens.sign(adminClaims()
                        .issueTime(Date.from(now.minusSeconds(7200)))
                        .expirationTime(Date.from(now.minusSeconds(3600))).build(), DevTokens.devPrivateKey())),
                Arguments.of("not valid yet", DevTokens.sign(adminClaims()
                        .notBeforeTime(Date.from(now.plusSeconds(3600))).build(), DevTokens.devPrivateKey())),
                Arguments.of("issued for another service", DevTokens.sign(adminClaims()
                        .audience("galactic-bank").build(), DevTokens.devPrivateKey())),
                Arguments.of("without audience", DevTokens.sign(adminClaims()
                        .audience((String) null).build(), DevTokens.devPrivateKey())),
                Arguments.of("issued by another provider", DevTokens.sign(adminClaims()
                        .issuer("https://evil.example").build(), DevTokens.devPrivateKey())),
                Arguments.of("signed by another key with the same key id", DevTokens.sign(adminClaims, attackerKey.getPrivate())),
                Arguments.of("signed with a shared secret (HS256)", signedWithSharedSecret.serialize()),
                Arguments.of("not signed (alg none)", new PlainJWT(adminClaims).serialize()),
                Arguments.of("payload replaced after signing", parts[0] + "." + payloadOfCat + "." + parts[2]),
                Arguments.of("signature cut off", parts[0] + "." + parts[1] + "."),
                Arguments.of("not a JWT", "definitely-not-a-token"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tokensThatMustBeRejected")
    void badTokenIsRejected(String description, String token) throws Exception {
        expectUnauthorized(mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer error=\"invalid_token\"")));
    }

    @Test
    void cosmoCatWorksOnlyWithOwnOrders() throws Exception {
        String productId = createProduct("Security test nebula snack", 5);
        String cartOfLuna = UUID.randomUUID().toString();

        // Luna cannot order in the name of Orion, and nothing is taken from the stock
        expectForbidden(mockMvc.perform(placeOrder(UUID.randomUUID().toString(), ORION_ID, productId)
                .with(token("COSMO_CAT", LUNA_EMAIL))));
        assertThat(stockOf(productId)).isEqualTo(5);

        mockMvc.perform(placeOrder(cartOfLuna, LUNA_ID, productId).with(token("COSMO_CAT", LUNA_EMAIL)))
                .andExpect(status().isCreated());
        assertThat(stockOf(productId)).isEqualTo(4);

        mockMvc.perform(get(ORDERS_URL + "/{cartId}", cartOfLuna).with(token("COSMO_CAT", LUNA_EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(LUNA_ID));
        expectForbidden(mockMvc.perform(get(ORDERS_URL + "/{cartId}", cartOfLuna).with(token("COSMO_CAT", ORION_EMAIL))));
        mockMvc.perform(get(ORDERS_URL + "/{cartId}", cartOfLuna).with(token("ADMIN", "admin@cosmo-cats.market")))
                .andExpect(status().isOk());
    }

    @Test
    void committeeReadsReportWithApiKeyAndNothingElse() throws Exception {
        mockMvc.perform(get(REPORT_URL).header("X-API-Key", COMMITTEE_KEY))
                .andExpect(status().isOk());

        expectUnauthorized(mockMvc.perform(get(REPORT_URL).header("X-API-Key", "wrong-key")));
        expectForbidden(mockMvc.perform(post(PRODUCTS_URL).header("X-API-Key", COMMITTEE_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson("Security test moon treat", 5))));
        expectForbidden(mockMvc.perform(placeOrder(UUID.randomUUID().toString(), LUNA_ID, UUID.randomUUID().toString())
                .header("X-API-Key", COMMITTEE_KEY)));
    }

    @Test
    void reportIsNotForCosmoCats() throws Exception {
        expectForbidden(mockMvc.perform(get(REPORT_URL).with(token("COSMO_CAT", LUNA_EMAIL))));
        mockMvc.perform(get(REPORT_URL).with(token("ADMIN", "admin@cosmo-cats.market")))
                .andExpect(status().isOk());
    }

    private String createProduct(String name, int stock) throws Exception {
        String response = mockMvc.perform(post(PRODUCTS_URL).with(token("ADMIN", "admin@cosmo-cats.market"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productJson(name, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private int stockOf(String productId) throws Exception {
        String response = mockMvc.perform(get(PRODUCTS_URL + "/{id}", productId).with(token("ADMIN", "admin@cosmo-cats.market")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("stockQuantity").asInt();
    }

    private static MockHttpServletRequestBuilder placeOrder(String cartId, String customerId, String productId) {
        return post(ORDERS_URL).contentType(MediaType.APPLICATION_JSON).content("""
                {"cartId": "%s", "customerId": "%s", "items": [{"productId": "%s", "quantity": 1}]}
                """.formatted(cartId, customerId, productId));
    }

    private static ResultActions expectUnauthorized(ResultActions result) throws Exception {
        return result
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "unauthorized"));
    }

    private static ResultActions expectForbidden(ResultActions result) throws Exception {
        return result
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERRORS_BASE_URL + "forbidden"))
                .andExpect(jsonPath("$.detail").value("You do not have permission to perform this action."));
    }

    private static RequestPostProcessor token(String role, String email) {
        String token = DevTokens.token(List.of(role), email);
        return request -> {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
            return request;
        };
    }

    private static JWTClaimsSet.Builder adminClaims() {
        return DevTokens.claims(List.of("ADMIN"), "admin@cosmo-cats.market");
    }

    private static String productJson(String name, int stock) {
        return """
                {"name": "%s", "price": 4.99, "stockQuantity": %d, "categoryId": "%s"}
                """.formatted(name, stock, COSMIC_FOOD_ID);
    }
}

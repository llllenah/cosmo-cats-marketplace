package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.AbstractIT;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The identity provider does not answer, so the application has no keys to check tokens with.
 * A separate class because the keys are cached after the first successful download:
 * here the application context never gets them.
 */
@WithAnonymousUser
@DisplayName("Identity provider is down")
class IdentityProviderDownIT extends AbstractIT {

    @BeforeEach
    void identityProviderIsDown() {
        identityProvider.stubFor(WireMock.get("/.well-known/jwks.json").atPriority(1).willReturn(serviceUnavailable()));
    }

    @Test
    void validTokenGets503InsteadOfRejection() throws Exception {
        String token = DevTokens.token(List.of("ADMIN"), "admin@cosmo-cats.market");

        mockMvc.perform(get("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/authentication-unavailable"));
    }

    @Test
    void slowIdentityProviderDoesNotHoldRequests() throws Exception {
        // The keys arrive only after 5 seconds. Without a timeout every request with a token would wait
        // that long, and a provider that hangs would take all request threads of the application.
        identityProvider.stubFor(WireMock.get("/.well-known/jwks.json").atPriority(1).willReturn(
                WireMock.aResponse().withStatus(200).withBodyFile("jwks.json").withFixedDelay(5_000)));
        String token = DevTokens.token(List.of("ADMIN"), "admin@cosmo-cats.market");

        long started = System.nanoTime();
        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/authentication-unavailable"));

        assertThat(Duration.ofNanos(System.nanoTime() - started)).isLessThan(Duration.ofSeconds(4));
    }

    @Test
    void apiKeyClientsAreNotAffected() throws Exception {
        // No key is configured in this test, so the answer is 401: the point is that it is not 503
        mockMvc.perform(get("/api/v1/products")
                        .header("X-API-Key", "some-key"))
                .andExpect(status().isUnauthorized());
    }
}

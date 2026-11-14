package com.cosmocats.marketplace;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Base class for integration tests (*IT): starts the whole application context with
 * PostgreSQL in Docker (Testcontainers). The external exchange rate service is replaced
 * by a WireMock server on a random port, so the context is recreated for every test class.
 * The database is shared by the test classes: tests create their own data with unique names
 * and do not expect an empty table.
 * <p>
 * Requests are sent as an admin, so the tests of the business logic do not deal with tokens.
 * Tests about security itself switch the mock user off with @WithAnonymousUser and send real tokens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
@WithMockUser(roles = "ADMIN")
public abstract class AbstractIT {

    @RegisterExtension
    protected static final WireMockExtension exchangeRateService = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    // The same mappings and JWKS file that the WireMock container from scripts/docker/compose.yaml
    // serves locally, so the tests also check that these files work
    @RegisterExtension
    protected static final WireMockExtension identityProvider = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().usingFilesUnderDirectory("scripts/wiremock"))
            .build();

    @Autowired
    protected MockMvc mockMvc;

    @DynamicPropertySource
    static void externalServices(DynamicPropertyRegistry registry) {
        PostgresTestContainer.registerDataSource(registry);
        registry.add("application.exchange-rate-service.base-url", exchangeRateService::baseUrl);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
                () -> identityProvider.baseUrl() + "/.well-known/jwks.json");
    }
}

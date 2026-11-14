package com.cosmocats.marketplace.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The development key in scripts/security is public, and the WireMock container from
 * scripts/docker/compose.yaml serves its JWKS on localhost:8090. So the application must trust
 * those keys only when it is told to (profile local, tests), never by default.
 */
@DisplayName("Identity provider configuration")
class IdentityProviderConfigurationTest {

    private static final String JWK_SET_URI = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri";
    private static final String ISSUER_URI = "spring.security.oauth2.resourceserver.jwt.issuer-uri";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    OAuth2ResourceServerAutoConfiguration.class,
                    OAuth2ClientAutoConfiguration.class,
                    JacksonAutoConfiguration.class,
                    HttpMessageConvertersAutoConfiguration.class,
                    WebMvcAutoConfiguration.class,
                    DispatcherServletAutoConfiguration.class))
            .withUserConfiguration(SecurityConfig.class)
            .withPropertyValues("spring.security.oauth2.client.registration.github.client-id=test-client-id",
                    "spring.security.oauth2.client.registration.github.client-secret=test-client-secret");

    @Test
    void defaultConfigurationDoesNotPointToAnyIdentityProvider() throws IOException {
        List<PropertySource<?>> defaults = new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"));

        assertThat(defaults).allSatisfy(source -> {
            assertThat(source.getProperty(JWK_SET_URI)).as(JWK_SET_URI).isNull();
            assertThat(source.getProperty(ISSUER_URI)).as(ISSUER_URI).isNull();
        });
    }

    @Test
    void applicationDoesNotStartWithoutIdentityProvider() {
        contextRunner.run(context -> assertThat(context)
                .hasFailed()
                .getFailure()
                .rootCause()
                .hasMessageContaining(JwtDecoder.class.getName()));
    }

    @Test
    void applicationStartsWhenIdentityProviderIsConfigured() {
        contextRunner
                .withPropertyValues(JWK_SET_URI + "=https://identity.example/.well-known/jwks.json",
                        ISSUER_URI + "=https://identity.example")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtDecoder.class));
    }
}

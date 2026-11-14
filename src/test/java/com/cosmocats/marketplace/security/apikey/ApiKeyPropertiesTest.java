package com.cosmocats.marketplace.security.apikey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiKeyProperties")
class ApiKeyPropertiesTest {

    private static final String HASH = "9c3ab4daf77d5eb2aa8db948f6fc5ae61db834d32b0029c9241728a7c6caf953";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
            .withUserConfiguration(PropertiesConfig.class);

    @Test
    void bindsClientsByName() {
        contextRunner
                .withPropertyValues(
                        "application.security.api-keys.intergalactic-committee.key-sha256=" + HASH,
                        "application.security.api-keys.intergalactic-committee.roles=COMMITTEE,AUDITOR",
                        "application.security.api-keys.not-ready-yet.key-sha256=")
                .run(context -> {
                    ApiKeyProperties properties = context.getBean(ApiKeyProperties.class);
                    assertThat(properties.apiKeys()).containsOnlyKeys("intergalactic-committee", "not-ready-yet");
                    ApiKeyProperties.Client committee = properties.apiKeys().get("intergalactic-committee");
                    assertThat(committee.keySha256()).isEqualTo(HASH);
                    assertThat(committee.roles()).containsExactlyInAnyOrder("COMMITTEE", "AUDITOR");
                    ApiKeyProperties.Client notReady = properties.apiKeys().get("not-ready-yet");
                    assertThat(notReady.keySha256()).isEmpty();
                    assertThat(notReady.roles()).isEmpty();
                });
    }

    @Test
    void hasNoClientsWhenNothingIsConfigured() {
        contextRunner.run(context -> assertThat(context.getBean(ApiKeyProperties.class).apiKeys()).isEmpty());
    }

    // The most likely mistake is to paste the key itself where its hash is expected.
    // The application must not start with it and silently reject every request of that client.
    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
            "committee-test-key",
            "9c3ab4daf77d5eb2aa8db948f6fc5ae61db834d32b0029c9241728a7c6caf95",
            "9c3ab4daf77d5eb2aa8db948f6fc5ae61db834d32b0029c9241728a7c6caf9530",
            "zz3ab4daf77d5eb2aa8db948f6fc5ae61db834d32b0029c9241728a7c6caf953"
    })
    void applicationDoesNotStartWithValueThatIsNotSha256(String keySha256) {
        contextRunner
                .withPropertyValues("application.security.api-keys.intergalactic-committee.key-sha256=" + keySha256)
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .hasMessageContaining("must be empty or a SHA-256 hash in hex (64 characters)"));
    }

    @Configuration
    @EnableConfigurationProperties(ApiKeyProperties.class)
    static class PropertiesConfig {
    }
}

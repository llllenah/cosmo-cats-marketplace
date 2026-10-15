package com.cosmocats.marketplace.application.cat;

import com.cosmocats.marketplace.domain.cat.CosmoCat;
import com.cosmocats.marketplace.featuretoggle.FeatureNotAvailableException;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleAspect;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleConfig;
import com.cosmocats.marketplace.featuretoggle.FeatureToggleService;
import com.cosmocats.marketplace.infrastructure.persistence.CosmoCatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * CosmoCatService with the real FeatureToggleAspect around it: every test starts a small
 * Spring context with only the beans needed for the toggle and its own feature flags.
 */
@DisplayName("CosmoCatService behind the cosmoCats feature toggle")
class CosmoCatServiceTest {

    private static final List<CosmoCat> COSMO_CATS = List.of(
            new CosmoCat(UUID.randomUUID(), "Luna", "Moon"),
            new CosmoCat(UUID.randomUUID(), "Orion", "Mars"));

    private final CosmoCatRepository cosmoCatRepository = mock(CosmoCatRepository.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AopAutoConfiguration.class))
            .withUserConfiguration(FeatureToggleConfig.class, FeatureToggleService.class,
                    FeatureToggleAspect.class, CosmoCatService.class)
            .withBean(CosmoCatRepository.class, () -> cosmoCatRepository);

    @Test
    void returnsCosmoCatsWhenFeatureIsEnabled() {
        when(cosmoCatRepository.findAll()).thenReturn(COSMO_CATS);

        contextRunner.withPropertyValues("feature.cosmoCats.enabled=true").run(context ->
                assertThat(context.getBean(CosmoCatService.class).getCosmoCats()).isEqualTo(COSMO_CATS));
    }

    @Test
    void throwsFeatureNotAvailableWhenFeatureIsDisabled() {
        contextRunner.withPropertyValues("feature.cosmoCats.enabled=false").run(context -> {
            CosmoCatService cosmoCatService = context.getBean(CosmoCatService.class);

            assertThatThrownBy(cosmoCatService::getCosmoCats)
                    .isInstanceOf(FeatureNotAvailableException.class)
                    .hasMessage("Feature 'cosmoCats' is not available.");
            verifyNoInteractions(cosmoCatRepository);
        });
    }

    @Test
    void throwsFeatureNotAvailableWhenFlagIsNotConfiguredAtAll() {
        contextRunner.run(context ->
                assertThatThrownBy(() -> context.getBean(CosmoCatService.class).getCosmoCats())
                        .isInstanceOf(FeatureNotAvailableException.class));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "feature.cosmo-cats.enabled=true",
            "feature.cosmocats.enabled=true"
    })
    void acceptsAllUsualSpellingsOfTheFlag(String property) {
        when(cosmoCatRepository.findAll()).thenReturn(COSMO_CATS);

        contextRunner.withPropertyValues(property).run(context ->
                assertThat(context.getBean(CosmoCatService.class).getCosmoCats()).hasSize(2));
    }

    @Test
    void environmentVariableOverridesFlagFromApplicationYml() {
        contextRunner
                .withInitializer(context -> {
                    MutablePropertySources sources = context.getEnvironment().getPropertySources();
                    // application.yml has the lowest priority, environment variables are above it
                    sources.addLast(new MapPropertySource("application.yml",
                            Map.of("feature.cosmoCats.enabled", "true")));
                    sources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                    Map.of("FEATURE_COSMOCATS_ENABLED", "false")));
                })
                .run(context -> assertThatThrownBy(() -> context.getBean(CosmoCatService.class).getCosmoCats())
                        .isInstanceOf(FeatureNotAvailableException.class));
    }

    @Test
    void otherFlagsDoNotAffectCosmoCats() {
        contextRunner.withPropertyValues("feature.cosmoCats.enabled=false", "feature.kittyProducts.enabled=true")
                .run(context -> assertThatThrownBy(() -> context.getBean(CosmoCatService.class).getCosmoCats())
                        .isInstanceOf(FeatureNotAvailableException.class));
    }
}

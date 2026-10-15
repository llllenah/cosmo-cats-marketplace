package com.cosmocats.marketplace.featuretoggle;

import com.cosmocats.marketplace.featuretoggle.FeatureToggleConfig.FeatureFlags;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FeatureToggleConfig and FeatureToggleService")
class FeatureToggleConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(FeatureToggleConfig.class, FeatureToggleService.class);

    @Test
    void readsAllFeatureFlagsIntoMap() {
        contextRunner.withPropertyValues("feature.cosmoCats.enabled=true", "feature.kittyProducts.enabled=false")
                .run(context -> {
                    assertThat(context.getBean(FeatureFlags.class).enabledByKey())
                            .containsExactlyInAnyOrderEntriesOf(Map.of("cosmocats", true, "kittyproducts", false));
                    FeatureToggleService service = context.getBean(FeatureToggleService.class);
                    assertThat(service.isEnabled(Feature.COSMO_CATS)).isTrue();
                    assertThat(service.isEnabled(Feature.KITTY_PRODUCTS)).isFalse();
                });
    }

    @Test
    void treatsFlagWithoutEnabledValueAsDisabled() {
        contextRunner.withPropertyValues("feature.cosmoCats.description=Cats list")
                .run(context -> assertThat(context.getBean(FeatureToggleService.class).isEnabled(Feature.COSMO_CATS))
                        .isFalse());
    }

    @Test
    void startsWithEmptyMapWhenNoFlagsAreConfigured() {
        contextRunner.run(context -> {
            assertThat(context.getBean(FeatureFlags.class).enabledByKey()).isEmpty();
            assertThat(context.getBean(FeatureToggleService.class).isEnabled(Feature.COSMO_CATS)).isFalse();
        });
    }

    @Test
    void failsOnStartupWhenFlagIsNotBoolean() {
        contextRunner.withPropertyValues("feature.cosmoCats.enabled=sometimes")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsOnStartupWhenOneFeatureHasConflictingValuesUnderDifferentSpellings() {
        contextRunner.withPropertyValues("feature.cosmoCats.enabled=true", "feature.cosmo-cats.enabled=false")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseMessage("The same feature flag is configured with different values "
                                + "under different spellings, e.g. cosmoCats and cosmo-cats"));
    }

    @Test
    void mergesSpellingsOfOneFeatureWithTheSameValue() {
        FeatureFlags flags = new FeatureFlags(Map.of("cosmoCats", false, "cosmocats", false));

        assertThat(flags.enabledByKey()).containsExactly(Map.entry("cosmocats", false));
    }

    @Test
    void featureFlagsMapCannotBeChangedFromOutside() {
        Map<String, Boolean> source = new HashMap<>(Map.of("cosmocats", true));
        FeatureFlags flags = new FeatureFlags(source);

        source.put("cosmocats", false);

        assertThat(flags.isEnabled("cosmoCats")).isTrue();
        assertThatThrownBy(() -> flags.enabledByKey().put("x", true)).isInstanceOf(UnsupportedOperationException.class);
    }
}

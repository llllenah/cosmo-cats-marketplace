package com.cosmocats.marketplace.featuretoggle;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class FeatureToggleConfig {

    private static final String FEATURE_PREFIX = "feature";

    /**
     * Reads all {@code feature.<key>.enabled} flags from the environment: application.yml, profiles,
     * environment variables (FEATURE_COSMOCATS_ENABLED) or command line arguments.
     */
    @Bean
    public FeatureFlags featureFlags(Environment environment) {
        Map<String, FeatureFlag> flags = Binder.get(environment)
                .bind(FEATURE_PREFIX, Bindable.mapOf(String.class, FeatureFlag.class))
                .orElse(Map.of());
        return new FeatureFlags(flags.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().enabled())));
    }

    record FeatureFlag(boolean enabled) {
    }

    /**
     * Map "feature key -> enabled". Keys are normalized, so cosmoCats, cosmo-cats and cosmocats
     * (the form that comes from an environment variable) are the same feature.
     */
    public record FeatureFlags(Map<String, Boolean> enabledByKey) {

        public FeatureFlags {
            // cosmoCats from application.yml and cosmocats from FEATURE_COSMOCATS_ENABLED come as two keys.
            // Binder already resolved both to the value with the highest priority, so they are equal.
            enabledByKey = enabledByKey.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            entry -> normalize(entry.getKey()),
                            Map.Entry::getValue,
                            FeatureFlags::sameValueOrFail));
        }

        private static Boolean sameValueOrFail(Boolean first, Boolean second) {
            if (!first.equals(second)) {
                throw new IllegalStateException("The same feature flag is configured with different values "
                        + "under different spellings, e.g. cosmoCats and cosmo-cats");
            }
            return first;
        }

        public boolean isEnabled(String key) {
            return enabledByKey.getOrDefault(normalize(key), false);
        }

        private static String normalize(String key) {
            return key.replace("-", "").toLowerCase(Locale.ROOT);
        }
    }
}

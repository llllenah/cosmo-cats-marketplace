package com.cosmocats.marketplace.featuretoggle;

import com.cosmocats.marketplace.featuretoggle.FeatureToggleConfig.FeatureFlags;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureToggleService {

    private final FeatureFlags featureFlags;

    /**
     * A feature that is not configured at all is disabled: a typo in the configuration
     * must not switch on unfinished functionality.
     */
    public boolean isEnabled(Feature feature) {
        return featureFlags.isEnabled(feature.key());
    }
}

package com.cosmocats.marketplace.featuretoggle;

public class FeatureNotAvailableException extends RuntimeException {

    public FeatureNotAvailableException(Feature feature) {
        super("Feature '%s' is not available.".formatted(feature.key()));
    }
}

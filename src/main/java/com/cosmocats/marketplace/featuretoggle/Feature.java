package com.cosmocats.marketplace.featuretoggle;

/**
 * Functions that can be switched on and off with {@code feature.<key>.enabled} in the configuration.
 */
public enum Feature {

    COSMO_CATS("cosmoCats"),
    KITTY_PRODUCTS("kittyProducts");

    private final String key;

    Feature(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}

package com.cosmocats.marketplace.application.exception;

import java.util.UUID;

public class CosmoCatNotFoundException extends RuntimeException {

    public CosmoCatNotFoundException(UUID cosmoCatId) {
        super("Cosmo cat with id '%s' does not exist.".formatted(cosmoCatId));
    }
}

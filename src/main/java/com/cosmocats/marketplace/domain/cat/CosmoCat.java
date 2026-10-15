package com.cosmocats.marketplace.domain.cat;

import java.util.Objects;
import java.util.UUID;

/**
 * A cat customer of the marketplace.
 */
public record CosmoCat(UUID id, String name, String homePlanet) {

    public CosmoCat {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(homePlanet, "homePlanet must not be null");
    }
}

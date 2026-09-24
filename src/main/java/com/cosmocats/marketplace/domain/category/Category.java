package com.cosmocats.marketplace.domain.category;

import java.util.Objects;
import java.util.UUID;

public record Category(UUID id, String name) {

    public Category {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}

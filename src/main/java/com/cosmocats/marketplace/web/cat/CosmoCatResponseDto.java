package com.cosmocats.marketplace.web.cat;

import java.util.UUID;

public record CosmoCatResponseDto(UUID id, String name, String homePlanet) {
}

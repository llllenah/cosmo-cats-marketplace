package com.cosmocats.marketplace.web.cat;

import com.cosmocats.marketplace.domain.cat.CosmoCat;
import org.springframework.stereotype.Component;

@Component
public class CosmoCatMapper {

    public CosmoCatResponseDto toDto(CosmoCat cosmoCat) {
        return new CosmoCatResponseDto(cosmoCat.id(), cosmoCat.name(), cosmoCat.homePlanet());
    }
}

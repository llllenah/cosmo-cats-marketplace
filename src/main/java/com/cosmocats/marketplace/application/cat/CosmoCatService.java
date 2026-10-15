package com.cosmocats.marketplace.application.cat;

import com.cosmocats.marketplace.domain.cat.CosmoCat;
import com.cosmocats.marketplace.featuretoggle.Feature;
import com.cosmocats.marketplace.featuretoggle.FeatureToggle;
import com.cosmocats.marketplace.infrastructure.persistence.CosmoCatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CosmoCatService {

    private final CosmoCatRepository cosmoCatRepository;

    @FeatureToggle(Feature.COSMO_CATS)
    public List<CosmoCat> getCosmoCats() {
        return cosmoCatRepository.findAll();
    }
}

package com.cosmocats.marketplace.application.cat;

import com.cosmocats.marketplace.domain.cat.CosmoCat;
import com.cosmocats.marketplace.featuretoggle.Feature;
import com.cosmocats.marketplace.featuretoggle.FeatureToggle;
import com.cosmocats.marketplace.infrastructure.persistence.CosmoCatRepository;
import com.cosmocats.marketplace.infrastructure.persistence.EntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CosmoCatService {

    private final CosmoCatRepository cosmoCatRepository;
    private final EntityMapper entityMapper;

    @FeatureToggle(Feature.COSMO_CATS)
    public List<CosmoCat> getCosmoCats() {
        return cosmoCatRepository.findAllByOrderByNameAsc().stream()
                .map(entityMapper::toDomain)
                .toList();
    }
}

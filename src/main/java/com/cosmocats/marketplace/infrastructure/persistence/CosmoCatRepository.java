package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.cat.CosmoCat;

import java.util.List;

public interface CosmoCatRepository {

    List<CosmoCat> findAll();
}

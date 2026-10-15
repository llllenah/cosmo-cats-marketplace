package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.cat.CosmoCat;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCosmoCatRepository implements CosmoCatRepository {

    private final Map<UUID, CosmoCat> cosmoCats = new ConcurrentHashMap<>();

    public InMemoryCosmoCatRepository() {
        MockCatalogData.cosmoCats().forEach(cat -> cosmoCats.put(cat.id(), cat));
    }

    @Override
    public List<CosmoCat> findAll() {
        return cosmoCats.values().stream()
                .sorted(Comparator.comparing(CosmoCat::name))
                .toList();
    }
}

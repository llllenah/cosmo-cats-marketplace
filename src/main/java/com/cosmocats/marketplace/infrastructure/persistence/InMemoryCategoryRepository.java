package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.category.Category;
import com.cosmocats.marketplace.domain.category.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryCategoryRepository implements CategoryRepository {

    private final Map<UUID, Category> categories = new ConcurrentHashMap<>();

    public InMemoryCategoryRepository() {
        MockCatalogData.categories().forEach(category -> categories.put(category.id(), category));
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return Optional.ofNullable(categories.get(id));
    }

    @Override
    public List<Category> findAll() {
        return List.copyOf(categories.values());
    }

    @Override
    public boolean existsById(UUID id) {
        return categories.containsKey(id);
    }
}

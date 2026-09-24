package com.cosmocats.marketplace.domain.category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    Optional<Category> findById(UUID id);

    List<Category> findAll();

    boolean existsById(UUID id);
}

package com.cosmocats.marketplace.domain.product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(UUID id);

    List<Product> findPage(int page, int size);

    long count();

    boolean existsById(UUID id);

    boolean existsByNameIgnoreCase(String name);

    void deleteById(UUID id);
}

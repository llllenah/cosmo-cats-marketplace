package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.application.product.PageCursor;
import com.cosmocats.marketplace.domain.product.Product;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private final Map<UUID, Product> products = new ConcurrentHashMap<>();

    public InMemoryProductRepository() {
        MockCatalogData.products().forEach(this::save);
    }

    @Override
    public Product save(Product product) {
        products.put(product.id(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public Optional<Product> findByNameIgnoreCase(String name) {
        return products.values().stream()
                .filter(product -> product.name().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<Product> findPageAfter(PageCursor cursor, int limit) {
        return products.values().stream()
                .filter(product -> cursor == null || cursor.isBefore(product))
                .sorted(PageCursor.PRODUCT_ORDER)
                .limit(limit)
                .toList();
    }

    @Override
    public long count() {
        return products.size();
    }

    @Override
    public boolean existsById(UUID id) {
        return products.containsKey(id);
    }

    @Override
    public void deleteById(UUID id) {
        products.remove(id);
    }
}

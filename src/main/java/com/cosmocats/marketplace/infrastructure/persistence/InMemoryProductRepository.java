package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.domain.product.ProductRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryProductRepository implements ProductRepository {

    private static final Comparator<Product> BY_NAME_THEN_ID = Comparator
            .comparing(Product::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(Product::id);

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
    public List<Product> findPage(int page, int size) {
        return products.values().stream()
                .sorted(BY_NAME_THEN_ID)
                .skip((long) page * size)
                .limit(size)
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
    public boolean existsByNameIgnoreCase(String name) {
        return products.values().stream()
                .anyMatch(product -> product.name().equalsIgnoreCase(name));
    }

    @Override
    public void deleteById(UUID id) {
        products.remove(id);
    }
}

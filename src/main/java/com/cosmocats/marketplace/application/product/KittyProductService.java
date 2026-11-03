package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.featuretoggle.Feature;
import com.cosmocats.marketplace.featuretoggle.FeatureToggle;
import com.cosmocats.marketplace.infrastructure.persistence.EntityMapper;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Products picked for kittens. The whole class is behind the kittyProducts flag.
 */
@Service
@FeatureToggle(Feature.KITTY_PRODUCTS)
@Transactional(readOnly = true)
public class KittyProductService {

    private final ProductRepository productRepository;
    private final EntityMapper entityMapper;
    private final UUID kittyCategoryId;

    public KittyProductService(ProductRepository productRepository,
                               EntityMapper entityMapper,
                               @Value("${application.kitty-products.category-id}") UUID kittyCategoryId) {
        this.productRepository = productRepository;
        this.entityMapper = entityMapper;
        this.kittyCategoryId = kittyCategoryId;
    }

    public List<Product> getKittyProducts() {
        return productRepository.findByCategoryReferenceOrderByNameAscReferenceAsc(kittyCategoryId).stream()
                .map(entityMapper::toDomain)
                .toList();
    }
}

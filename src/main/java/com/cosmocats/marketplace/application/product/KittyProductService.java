package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.featuretoggle.Feature;
import com.cosmocats.marketplace.featuretoggle.FeatureToggle;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Products picked for kittens. The whole class is behind the kittyProducts flag.
 */
@Service
@FeatureToggle(Feature.KITTY_PRODUCTS)
public class KittyProductService {

    private final ProductRepository productRepository;
    private final UUID kittyCategoryId;

    public KittyProductService(ProductRepository productRepository,
                               @Value("${application.kitty-products.category-id}") UUID kittyCategoryId) {
        this.productRepository = productRepository;
        this.kittyCategoryId = kittyCategoryId;
    }

    public List<Product> getKittyProducts() {
        return productRepository.findByCategoryId(kittyCategoryId);
    }
}

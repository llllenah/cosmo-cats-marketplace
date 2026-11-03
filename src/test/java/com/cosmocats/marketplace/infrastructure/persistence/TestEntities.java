package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.order.OrderStatus;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CosmoCatEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entities for tests. Numeric ids are left empty: in unit tests nobody reads them,
 * in repository tests the sequences generate them.
 */
public final class TestEntities {

    private TestEntities() {
    }

    public static CategoryEntity category(UUID reference, String name) {
        CategoryEntity category = new CategoryEntity();
        category.setReference(reference);
        category.setName(name);
        return category;
    }

    public static ProductEntity product(UUID reference, String name, String price, int stockQuantity,
                                        CategoryEntity category) {
        ProductEntity product = new ProductEntity();
        product.setReference(reference);
        product.setName(name);
        product.setDescription("Description of " + name);
        product.setPrice(new BigDecimal(price));
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        return product;
    }

    public static CosmoCatEntity cosmoCat(UUID reference, String name, String email) {
        CosmoCatEntity cosmoCat = new CosmoCatEntity();
        cosmoCat.setReference(reference);
        cosmoCat.setName(name);
        cosmoCat.setEmail(email);
        cosmoCat.setHomePlanet("Moon");
        return cosmoCat;
    }

    public static OrderEntity order(UUID cartId, CosmoCatEntity cosmoCat, OrderStatus status, Instant createdAt) {
        OrderEntity order = new OrderEntity();
        order.setCartId(cartId);
        order.setCosmoCat(cosmoCat);
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        return order;
    }
}

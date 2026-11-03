package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.cat.CosmoCat;
import com.cosmocats.marketplace.domain.order.Order;
import com.cosmocats.marketplace.domain.order.OrderItem;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CosmoCatEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import org.springframework.stereotype.Component;

/**
 * Maps JPA entities to domain objects. Entities do not leave the service layer:
 * the domain objects use public UUIDs, the numeric ids stay in the database.
 * Must be called inside a transaction, because related entities are loaded lazily.
 */
@Component
public class EntityMapper {

    public Product toDomain(ProductEntity product) {
        return new Product(
                product.getReference(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCategory().getReference()
        );
    }

    public CosmoCat toDomain(CosmoCatEntity cosmoCat) {
        return new CosmoCat(cosmoCat.getReference(), cosmoCat.getName(), cosmoCat.getEmail(), cosmoCat.getHomePlanet());
    }

    public Order toDomain(OrderEntity order) {
        return new Order(
                order.getCartId(),
                order.getCosmoCat().getReference(),
                order.getItems().stream()
                        .map(item -> new OrderItem(item.getProduct().getReference(), item.getQuantity(), item.getUnitPrice()))
                        .toList(),
                order.getStatus(),
                order.getCreatedAt()
        );
    }
}

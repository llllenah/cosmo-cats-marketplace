package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.cat.CosmoCat;
import com.cosmocats.marketplace.domain.order.Order;
import com.cosmocats.marketplace.domain.order.OrderItem;
import com.cosmocats.marketplace.domain.order.OrderStatus;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CosmoCatEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.category;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.cosmoCat;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.order;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.product;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EntityMapper")
class EntityMapperTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID CAT_ID = UUID.randomUUID();

    private final EntityMapper mapper = new EntityMapper();
    private final CategoryEntity food = category(CATEGORY_ID, "Cosmic food");

    @Test
    void mapsProductWithPublicIdsOnly() {
        ProductEntity entity = product(PRODUCT_ID, "Galaxy cosmic milk", "4.99", 300, food);
        entity.setId(42L);

        assertThat(mapper.toDomain(entity)).isEqualTo(new Product(PRODUCT_ID, "Galaxy cosmic milk",
                "Description of Galaxy cosmic milk", new BigDecimal("4.99"), 300, CATEGORY_ID));
    }

    @Test
    void mapsCosmoCat() {
        CosmoCatEntity entity = cosmoCat(CAT_ID, "Luna", "luna@moon.space");

        assertThat(mapper.toDomain(entity)).isEqualTo(new CosmoCat(CAT_ID, "Luna", "luna@moon.space", "Moon"));
    }

    @Test
    void mapsOrderWithItemPricesFromOrderNotFromCurrentProductPrice() {
        UUID cartId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-03T12:00:00Z");
        ProductEntity milk = product(PRODUCT_ID, "Galaxy cosmic milk", "5.50", 300, food);
        OrderEntity entity = order(cartId, cosmoCat(CAT_ID, "Luna", "luna@moon.space"), OrderStatus.SHIPPED, createdAt);
        entity.addItem(milk, 2, new BigDecimal("4.99"));

        Order order = mapper.toDomain(entity);

        assertThat(order).isEqualTo(new Order(cartId, CAT_ID,
                List.of(new OrderItem(PRODUCT_ID, 2, new BigDecimal("4.99"))), OrderStatus.SHIPPED, createdAt));
        assertThat(order.totalPrice()).isEqualByComparingTo("9.98");
    }
}

package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.order.OrderStatus;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CosmoCatEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.infrastructure.persistence.projection.PopularProductView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.category;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.cosmoCat;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.order;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

@DisplayName("OrderRepository")
class OrderRepositoryIT extends AbstractRepositoryIT {

    private static final Set<OrderStatus> NOT_CANCELLED = EnumSet.complementOf(EnumSet.of(OrderStatus.CANCELLED));
    // PostgreSQL keeps microseconds, so the test time is truncated to compare it after reading
    private static final Instant CREATED_AT = Instant.now().truncatedTo(ChronoUnit.MICROS);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CosmoCatRepository cosmoCatRepository;

    private CosmoCatEntity luna;
    private ProductEntity milk;
    private ProductEntity capsule;
    private ProductEntity collar;

    @BeforeEach
    void setUp() {
        CategoryEntity category = categoryRepository.save(category(UUID.randomUUID(), "Test category " + UUID.randomUUID()));
        luna = cosmoCatRepository.save(cosmoCat(UUID.randomUUID(), "Test Luna", UUID.randomUUID() + "@moon.space"));
        milk = productRepository.save(product(UUID.randomUUID(), "Test galaxy milk", "4.99", 100, category));
        capsule = productRepository.save(product(UUID.randomUUID(), "Test nebula capsule", "89.90", 100, category));
        collar = productRepository.save(product(UUID.randomUUID(), "Test comet collar", "15.00", 100, category));
    }

    @Test
    void savesOrderWithItemsAndFindsItByCartId() {
        UUID cartId = UUID.randomUUID();
        OrderEntity order = order(cartId, luna, OrderStatus.CREATED, CREATED_AT);
        order.addItem(milk, 2, new BigDecimal("4.99"));
        order.addItem(capsule, 1, new BigDecimal("89.90"));
        orderRepository.save(order);
        flushAndClear();

        OrderEntity found = orderRepository.findByNaturalId(cartId).orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getCosmoCat().getReference()).isEqualTo(luna.getReference());
        assertThat(found.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(found.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(found.getItems())
                .extracting(item -> item.getProduct().getReference(), item -> item.getQuantity(), item -> item.getUnitPrice())
                .containsExactlyInAnyOrder(
                        tuple(milk.getReference(), 2, new BigDecimal("4.99")),
                        tuple(capsule.getReference(), 1, new BigDecimal("89.90")));
    }

    @Test
    void returnsEmptyForUnknownCart() {
        assertThat(orderRepository.findByNaturalId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void updatesStatus() {
        UUID cartId = saveOrder(OrderStatus.CREATED, milk, 1);

        orderRepository.findByNaturalId(cartId).orElseThrow().setStatus(OrderStatus.PAID);
        flushAndClear();

        assertThat(orderRepository.findByNaturalId(cartId)).get()
                .extracting(OrderEntity::getStatus).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void deletesOrderTogetherWithItems() {
        UUID cartId = saveOrder(OrderStatus.CREATED, milk, 1);
        Long orderId = orderRepository.findByNaturalId(cartId).orElseThrow().getId();

        orderRepository.delete(orderRepository.findByNaturalId(cartId).orElseThrow());
        flushAndClear();

        assertThat(orderRepository.findById(orderId)).isEmpty();
        assertThat(entityManager.getEntityManager()
                .createQuery("select count(i) from OrderItemEntity i where i.order.id = :orderId", Long.class)
                .setParameter("orderId", orderId)
                .getSingleResult()).isZero();
        assertThat(orderRepository.existsByItemsProductReference(milk.getReference())).isFalse();
    }

    @Test
    void rejectsSecondOrderForSameCart() {
        UUID cartId = saveOrder(OrderStatus.CREATED, milk, 1);
        OrderEntity duplicate = order(cartId, luna, OrderStatus.CREATED, CREATED_AT);
        duplicate.addItem(capsule, 1, new BigDecimal("89.90"));

        assertThatThrownBy(() -> orderRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void knowsWhichProductsAreOrdered() {
        saveOrder(OrderStatus.CANCELLED, milk, 1);

        assertThat(orderRepository.existsByItemsProductReference(milk.getReference())).isTrue();
        assertThat(orderRepository.existsByItemsProductReference(capsule.getReference())).isFalse();
    }

    private void placeOrdersForReport() {
        saveOrder(OrderStatus.CREATED, milk, 5, capsule, 1);
        saveOrder(OrderStatus.DELIVERED, milk, 1, collar, 3);
        saveOrder(OrderStatus.PAID, collar, 2);
        // Would make the collar the most popular product if cancelled orders were counted
        saveOrder(OrderStatus.CANCELLED, collar, 50);
    }

    @Test
    void sumsQuantitiesAndCountsOrdersWithoutCancelledOnes() {
        placeOrdersForReport();

        List<PopularProductView> report = ourProducts(orderRepository.findPopularProducts(NOT_CANCELLED, Limit.unlimited()));

        assertThat(report)
                .extracting(PopularProductView::getProductName, PopularProductView::getTotalQuantity,
                        PopularProductView::getOrderCount)
                .containsExactly(
                        tuple("Test galaxy milk", 6L, 2L),
                        tuple("Test comet collar", 5L, 2L),
                        tuple("Test nebula capsule", 1L, 1L));
        assertThat(report.get(0).getProductId()).isEqualTo(milk.getReference());
    }

    @Test
    void countsOnlyRequestedStatuses() {
        placeOrdersForReport();

        List<PopularProductView> report = ourProducts(orderRepository.findPopularProducts(
                EnumSet.of(OrderStatus.CANCELLED), Limit.unlimited()));

        assertThat(report).extracting(PopularProductView::getProductName, PopularProductView::getTotalQuantity)
                .containsExactly(tuple("Test comet collar", 50L));
    }

    @Test
    void returnsNoMoreRowsThanLimit() {
        placeOrdersForReport();

        assertThat(orderRepository.findPopularProducts(NOT_CANCELLED, Limit.of(2))).hasSize(2);
    }

    // Other tests may have committed orders to the shared database, they are left out
    private List<PopularProductView> ourProducts(List<PopularProductView> report) {
        Set<UUID> ours = Set.of(milk.getReference(), capsule.getReference(), collar.getReference());
        return report.stream().filter(row -> ours.contains(row.getProductId())).toList();
    }

    private UUID saveOrder(OrderStatus status, Object... productsAndQuantities) {
        UUID cartId = UUID.randomUUID();
        OrderEntity order = order(cartId, luna, status, CREATED_AT);
        for (int i = 0; i < productsAndQuantities.length; i += 2) {
            ProductEntity product = (ProductEntity) productsAndQuantities[i];
            order.addItem(product, (Integer) productsAndQuantities[i + 1], product.getPrice());
        }
        orderRepository.save(order);
        flushAndClear();
        return cartId;
    }
}

package com.cosmocats.marketplace.domain.order;

import com.cosmocats.marketplace.domain.cart.Cart;
import com.cosmocats.marketplace.domain.cart.CartItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Order and OrderItem")
class OrderTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final Instant CREATED_AT = Instant.parse("2026-10-14T10:15:30Z");

    @Test
    void placeFromCartCopiesItemsWithPricesAndStartsAsCreated() {
        UUID catnipId = UUID.randomUUID();
        UUID helmetId = UUID.randomUUID();
        Cart cart = new Cart(UUID.randomUUID(), CUSTOMER_ID, List.of(
                new CartItem(catnipId, 2, new BigDecimal("7.25")),
                new CartItem(helmetId, 1, new BigDecimal("39.00"))
        ));

        Order order = Order.placeFrom(cart, CREATED_AT);

        assertThat(order.id()).isNotNull();
        assertThat(order.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.createdAt()).isEqualTo(CREATED_AT);
        assertThat(order.items()).containsExactly(
                new OrderItem(catnipId, 2, new BigDecimal("7.25")),
                new OrderItem(helmetId, 1, new BigDecimal("39.00")));
        assertThat(order.totalPrice()).isEqualByComparingTo("53.50").isEqualByComparingTo(cart.totalPrice());
    }

    @Test
    void cannotPlaceOrderFromEmptyCart() {
        Cart emptyCart = new Cart(UUID.randomUUID(), CUSTOMER_ID, List.of());

        assertThatThrownBy(() -> Order.placeFrom(emptyCart, CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("cannot place an order from an empty cart");
    }

    @Test
    void orderMustContainItems() {
        assertThatThrownBy(() -> new Order(UUID.randomUUID(), CUSTOMER_ID, List.of(), OrderStatus.CREATED, CREATED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("order must contain at least one item");
    }

    @Test
    void orderRequiresAllMandatoryFields() {
        List<OrderItem> items = List.of(new OrderItem(UUID.randomUUID(), 1, BigDecimal.ONE));

        assertThatThrownBy(() -> new Order(null, CUSTOMER_ID, items, OrderStatus.PAID, CREATED_AT))
                .hasMessage("id must not be null");
        assertThatThrownBy(() -> new Order(UUID.randomUUID(), null, items, OrderStatus.PAID, CREATED_AT))
                .hasMessage("customerId must not be null");
        assertThatThrownBy(() -> new Order(UUID.randomUUID(), CUSTOMER_ID, items, null, CREATED_AT))
                .hasMessage("status must not be null");
        assertThatThrownBy(() -> new Order(UUID.randomUUID(), CUSTOMER_ID, items, OrderStatus.PAID, null))
                .hasMessage("createdAt must not be null");
    }

    @Test
    void orderItemValidatesItsFields() {
        assertThatThrownBy(() -> new OrderItem(null, 1, BigDecimal.ONE)).hasMessage("productId must not be null");
        assertThatThrownBy(() -> new OrderItem(UUID.randomUUID(), 1, null)).hasMessage("unitPrice must not be null");
        assertThatThrownBy(() -> new OrderItem(UUID.randomUUID(), 0, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be greater than 0");
    }
}

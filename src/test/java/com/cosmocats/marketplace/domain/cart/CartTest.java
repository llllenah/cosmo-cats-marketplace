package com.cosmocats.marketplace.domain.cart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cart and CartItem")
class CartTest {

    private static final UUID CART_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Test
    void totalPriceIsSumOfItemSubtotalsWithoutRoundingErrors() {
        Cart cart = new Cart(CART_ID, CUSTOMER_ID, List.of(
                new CartItem(UUID.randomUUID(), 3, new BigDecimal("0.10")),
                new CartItem(UUID.randomUUID(), 1, new BigDecimal("0.20"))
        ));

        assertThat(cart.totalPrice()).isEqualByComparingTo("0.50");
        assertThat(cart.isEmpty()).isFalse();
    }

    @Test
    void emptyCartCostsNothing() {
        Cart cart = new Cart(CART_ID, CUSTOMER_ID, List.of());

        assertThat(cart.totalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void itemsCannotBeChangedFromOutside() {
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(UUID.randomUUID(), 1, BigDecimal.ONE));
        Cart cart = new Cart(CART_ID, CUSTOMER_ID, items);

        items.clear();

        assertThat(cart.items()).hasSize(1);
        assertThatThrownBy(() -> cart.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiresIdAndCustomer() {
        assertThatThrownBy(() -> new Cart(null, CUSTOMER_ID, List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("id must not be null");
        assertThatThrownBy(() -> new Cart(CART_ID, null, List.of()))
                .isInstanceOf(NullPointerException.class).hasMessage("customerId must not be null");
    }

    @Test
    void itemSubtotalIsUnitPriceTimesQuantity() {
        assertThat(new CartItem(UUID.randomUUID(), 4, new BigDecimal("2.25")).subtotal())
                .isEqualByComparingTo("9.00");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void itemQuantityMustBePositive(int quantity) {
        assertThatThrownBy(() -> new CartItem(UUID.randomUUID(), quantity, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be greater than 0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.01"})
    void itemUnitPriceMustBePositive(String unitPrice) {
        assertThatThrownBy(() -> new CartItem(UUID.randomUUID(), 1, new BigDecimal(unitPrice)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unitPrice must be greater than 0");
    }

    @Test
    void itemRequiresProductAndPrice() {
        assertThatThrownBy(() -> new CartItem(null, 1, BigDecimal.ONE))
                .isInstanceOf(NullPointerException.class).hasMessage("productId must not be null");
        assertThatThrownBy(() -> new CartItem(UUID.randomUUID(), 1, null))
                .isInstanceOf(NullPointerException.class).hasMessage("unitPrice must not be null");
    }
}

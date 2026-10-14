package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PageCursor and PagedResult")
class PageCursorTest {

    private static final UUID FIRST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void afterPointsToGivenProduct() {
        Product product = product(FIRST_ID, "Star ball");

        assertThat(PageCursor.after(product)).isEqualTo(new PageCursor("Star ball", FIRST_ID));
    }

    @Test
    void comparesNamesIgnoringCase() {
        PageCursor cursor = new PageCursor("comet ball", FIRST_ID);

        assertThat(cursor.isBefore(product(SECOND_ID, "Moon ball"))).isTrue();
        assertThat(cursor.isBefore(product(SECOND_ID, "Asteroid ball"))).isFalse();
    }

    @Test
    void usesIdWhenNamesAreEqual() {
        PageCursor cursor = new PageCursor("Star ball", FIRST_ID);

        assertThat(cursor.isBefore(product(SECOND_ID, "STAR BALL"))).isTrue();
        assertThat(cursor.isBefore(product(FIRST_ID, "Star ball"))).isFalse();
    }

    @Test
    void isBeforeAgreesWithProductOrder() {
        List<Product> products = new ArrayList<>(List.of(
                product(SECOND_ID, "star ball"),
                product(FIRST_ID, "Star ball"),
                product(FIRST_ID, "Asteroid ball"),
                product(SECOND_ID, "Moon ball")));

        products.sort(PageCursor.PRODUCT_ORDER);

        for (int i = 0; i + 1 < products.size(); i++) {
            assertThat(PageCursor.after(products.get(i)).isBefore(products.get(i + 1))).isTrue();
            assertThat(PageCursor.after(products.get(i + 1)).isBefore(products.get(i))).isFalse();
        }
    }

    @Test
    void requiresNameAndId() {
        assertThatThrownBy(() -> new PageCursor(null, FIRST_ID)).hasMessage("name must not be null");
        assertThatThrownBy(() -> new PageCursor("Star ball", null)).hasMessage("id must not be null");
    }

    @Test
    void pagedResultContentIsImmutableCopy() {
        List<String> content = new ArrayList<>(List.of("a"));

        PagedResult<String> page = new PagedResult<>(content, 1, 1, null);
        content.add("b");

        assertThat(page.content()).containsExactly("a");
        assertThatThrownBy(() -> page.content().add("c")).isInstanceOf(UnsupportedOperationException.class);
    }

    private static Product product(UUID id, String name) {
        return new Product(id, name, null, BigDecimal.ONE, 1, UUID.randomUUID());
    }
}

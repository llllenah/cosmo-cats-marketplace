package com.cosmocats.marketplace.domain;

import com.cosmocats.marketplace.domain.cat.CosmoCat;
import com.cosmocats.marketplace.domain.category.Category;
import com.cosmocats.marketplace.domain.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product, Category and CosmoCat")
class DomainRecordsTest {

    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    void withIdReturnsCopyAndKeepsOriginalUnchanged() {
        Product product = new Product(null, "Star ball", "Soft", BigDecimal.TEN, 3, CATEGORY_ID);
        UUID newId = UUID.randomUUID();

        Product withId = product.withId(newId);

        assertThat(withId).isEqualTo(new Product(newId, "Star ball", "Soft", BigDecimal.TEN, 3, CATEGORY_ID));
        assertThat(product.id()).isNull();
    }

    @Test
    void productRequiresNamePriceAndCategory() {
        assertThatThrownBy(() -> new Product(null, null, null, BigDecimal.TEN, 1, CATEGORY_ID))
                .hasMessage("name must not be null");
        assertThatThrownBy(() -> new Product(null, "Star ball", null, null, 1, CATEGORY_ID))
                .hasMessage("price must not be null");
        assertThatThrownBy(() -> new Product(null, "Star ball", null, BigDecimal.TEN, 1, null))
                .hasMessage("categoryId must not be null");
    }

    @Test
    void categoryRequiresIdAndNonBlankName() {
        assertThat(new Category(CATEGORY_ID, "Space gear").name()).isEqualTo("Space gear");
        assertThatThrownBy(() -> new Category(null, "Space gear")).hasMessage("id must not be null");
        assertThatThrownBy(() -> new Category(CATEGORY_ID, null)).hasMessage("name must not be null");
        assertThatThrownBy(() -> new Category(CATEGORY_ID, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be blank");
    }

    @Test
    void cosmoCatRequiresAllFields() {
        assertThat(new CosmoCat(CATEGORY_ID, "Luna", "Moon").homePlanet()).isEqualTo("Moon");
        assertThatThrownBy(() -> new CosmoCat(null, "Luna", "Moon")).hasMessage("id must not be null");
        assertThatThrownBy(() -> new CosmoCat(CATEGORY_ID, null, "Moon")).hasMessage("name must not be null");
        assertThatThrownBy(() -> new CosmoCat(CATEGORY_ID, "Luna", null)).hasMessage("homePlanet must not be null");
    }
}

package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.category.Category;
import com.cosmocats.marketplace.domain.product.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Mock data used until the database integration is added (Lab 3).
 * Ids are fixed so that the examples in README can be reused.
 */
final class MockCatalogData {

    static final UUID ANTI_GRAVITY_TOYS_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID COSMIC_FOOD_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final UUID SPACE_GEAR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private MockCatalogData() {
    }

    static List<Category> categories() {
        return List.of(
                new Category(ANTI_GRAVITY_TOYS_ID, "Anti-gravity toys"),
                new Category(COSMIC_FOOD_ID, "Cosmic food"),
                new Category(SPACE_GEAR_ID, "Space gear")
        );
    }

    static List<Product> products() {
        return List.of(
                new Product(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
                        "Anti-gravity star yarn ball",
                        "Floats in zero gravity and never gets lost under the sofa.",
                        new BigDecimal("12.50"), 120, ANTI_GRAVITY_TOYS_ID),
                new Product(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002"),
                        "Galaxy cosmic milk",
                        "Lactose-free milk from the Milky Way farms.",
                        new BigDecimal("4.99"), 300, COSMIC_FOOD_ID),
                new Product(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000003"),
                        "Comet tail scratching post",
                        "Leaves a sparkling trail after every scratch.",
                        new BigDecimal("39.00"), 25, SPACE_GEAR_ID),
                new Product(UUID.fromString("aaaaaaaa-0000-0000-0000-000000000004"),
                        "Nebula nap capsule",
                        "Warm sleeping pod with a starry sky projection.",
                        new BigDecimal("89.90"), 10, SPACE_GEAR_ID)
        );
    }
}

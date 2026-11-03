package com.cosmocats.marketplace.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the schema created by the Liquibase changelog with plain SQL, without JPA.
 * PostgreSQL aborts a transaction after an error, so every constraint gets its own test.
 */
@DisplayName("Liquibase migrations")
class LiquibaseMigrationIT extends AbstractRepositoryIT {

    private static final String GALAXY_MILK_CATEGORY = "22222222-2222-2222-2222-222222222222";
    private static final String SPACE_GEAR_CATEGORY = "33333333-3333-3333-3333-333333333333";

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void appliesAllChangeSetsInOrderAndTagsVersions() {
        assertThat(jdbc.queryForList("SELECT id FROM databasechangelog ORDER BY orderexecuted", String.class))
                .containsExactly(
                        "001-create-category-table",
                        "001-create-product-table",
                        "002-create-cosmo-cat-table",
                        "002-create-orders-table",
                        "002-create-order-item-table",
                        "tag-1.0.0",
                        "003-product-name-unique-in-category",
                        "004-cosmo-cat-email-unique",
                        "005-seed-catalog-and-cosmo-cats",
                        "tag-1.1.0");
        assertThat(jdbc.queryForList(
                "SELECT tag FROM databasechangelog WHERE tag IS NOT NULL ORDER BY orderexecuted", String.class))
                .containsExactly("1.0.0", "1.1.0");
    }

    @Test
    void createsTablesSequencesAndIndexes() {
        assertThat(jdbc.queryForList("""
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name NOT LIKE 'databasechangelog%'
                """, String.class))
                .containsExactlyInAnyOrder("category", "product", "cosmo_cat", "orders", "order_item");

        // The same step as allocationSize of the entities, otherwise Hibernate would hand out duplicate ids
        assertThat(jdbc.queryForList("SELECT sequencename || ':' || increment_by FROM pg_sequences", String.class))
                .containsExactlyInAnyOrder("category_id_seq:50", "product_id_seq:50", "cosmo_cat_id_seq:50",
                        "orders_id_seq:50", "order_item_id_seq:50");

        assertThat(jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname = 'public'", String.class))
                .contains("idx_product_category_id", "uq_product_category_name", "idx_product_name_lower",
                        "idx_orders_cosmo_cat_id", "idx_order_item_product_id", "uq_order_item_order_product",
                        "uq_orders_cart_id", "uq_cosmo_cat_email");
    }

    @Test
    void nameIndexIsBuiltOnLowerCaseName() {
        assertThat(jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = 'uq_product_category_name'",
                String.class))
                .startsWith("CREATE UNIQUE INDEX")
                .contains("(category_id, lower((name)::text))");
    }

    @Test
    void seedsCatalogAndCosmoCats() {
        assertThat(jdbc.queryForList("""
                SELECT p.name || ' / ' || c.name FROM product p JOIN category c ON c.id = p.category_id
                WHERE p.reference::text LIKE 'aaaaaaaa-%' ORDER BY p.reference
                """, String.class))
                .containsExactly(
                        "Anti-gravity star yarn ball / Anti-gravity toys",
                        "Galaxy cosmic milk / Cosmic food",
                        "Comet tail scratching post / Space gear",
                        "Nebula nap capsule / Space gear");
        assertThat(jdbc.queryForList("SELECT email FROM cosmo_cat WHERE reference::text LIKE 'cccccccc-%' ORDER BY name",
                String.class))
                .containsExactly("luna@moon.space", "nova@kepler.space", "orion@mars.space");
    }

    @Test
    void rejectsSameProductNameInSameCategoryIgnoringCase() {
        assertThatThrownBy(() -> insertProduct("GALAXY COSMIC MILK", "1.00", GALAXY_MILK_CATEGORY))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_product_category_name");
    }

    @Test
    void allowsSameProductNameInAnotherCategory() {
        assertThatNoException().isThrownBy(() -> insertProduct("Galaxy cosmic milk", "1.00", SPACE_GEAR_CATEGORY));
    }

    @Test
    void rejectsSecondCosmoCatWithSameEmail() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO cosmo_cat (id, reference, name, email, home_planet)
                VALUES (nextval('cosmo_cat_id_seq'), gen_random_uuid(), 'Luna II', 'luna@moon.space', 'Moon')
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_cosmo_cat_email");
    }

    @Test
    void rejectsProductWithZeroPrice() {
        assertThatThrownBy(() -> insertProduct("Free star", "0.00", SPACE_GEAR_CATEGORY))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_product_price_positive");
    }

    @Test
    void rejectsDeletingCategoryThatHasProducts() {
        assertThatThrownBy(() -> jdbc.update("DELETE FROM category WHERE reference = ?::uuid", SPACE_GEAR_CATEGORY))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_product_category");
    }

    @Test
    void rejectsOrderStatusOutsideOfEnum() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO orders (id, cart_id, cosmo_cat_id, status, created_at)
                VALUES (nextval('orders_id_seq'), gen_random_uuid(), (SELECT min(id) FROM cosmo_cat), 'LOST', now())
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_orders_status");
    }

    private void insertProduct(String name, String price, String categoryReference) {
        jdbc.update("""
                INSERT INTO product (id, reference, name, price, stock_quantity, category_id)
                VALUES (nextval('product_id_seq'), gen_random_uuid(), ?, ?::numeric, 1,
                        (SELECT id FROM category WHERE reference = ?::uuid))
                """, name, price, categoryReference);
    }
}

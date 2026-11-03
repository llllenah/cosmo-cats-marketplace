package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.category;
import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductRepository")
class ProductRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private CategoryEntity toys;
    private CategoryEntity food;

    @BeforeEach
    void setUp() {
        toys = categoryRepository.save(category(UUID.randomUUID(), "Test toys " + UUID.randomUUID()));
        food = categoryRepository.save(category(UUID.randomUUID(), "Test food " + UUID.randomUUID()));
    }

    @Test
    void createsProductWithIdFromSequenceAndReadsItWithCategory() {
        UUID reference = UUID.randomUUID();
        ProductEntity saved = productRepository.save(product(reference, "Moon dust catnip", "7.25", 50, toys));
        flushAndClear();

        assertThat(saved.getId()).isNotNull();
        ProductEntity found = productRepository.findByReference(reference).orElseThrow();
        assertThat(found.getName()).isEqualTo("Moon dust catnip");
        assertThat(found.getPrice()).isEqualByComparingTo("7.25");
        assertThat(found.getStockQuantity()).isEqualTo(50);
        assertThat(found.getVersion()).isZero();
        // Loaded by the entity graph, no extra query when the mapper reads the category
        assertThat(Hibernate.isInitialized(found.getCategory())).isTrue();
        assertThat(found.getCategory().getReference()).isEqualTo(toys.getReference());
    }

    @Test
    void updatesProductAndIncreasesVersion() {
        UUID reference = UUID.randomUUID();
        productRepository.save(product(reference, "Moon dust catnip", "7.25", 50, toys));
        flushAndClear();

        ProductEntity product = productRepository.findByReference(reference).orElseThrow();
        product.setPrice(new BigDecimal("8.00"));
        product.setCategory(categoryRepository.findByReference(food.getReference()).orElseThrow());
        flushAndClear();

        ProductEntity updated = productRepository.findByReference(reference).orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo("8.00");
        assertThat(updated.getCategory().getReference()).isEqualTo(food.getReference());
        assertThat(updated.getVersion()).isEqualTo(1);
    }

    @Test
    void deletesProduct() {
        UUID reference = UUID.randomUUID();
        productRepository.save(product(reference, "Moon dust catnip", "7.25", 50, toys));
        flushAndClear();

        productRepository.delete(productRepository.findByReference(reference).orElseThrow());
        flushAndClear();

        assertThat(productRepository.findByReference(reference)).isEmpty();
    }

    @Test
    void rejectsStaleUpdateFromAnotherTransaction() {
        UUID reference = UUID.randomUUID();
        productRepository.save(product(reference, "Moon dust catnip", "7.25", 50, toys));
        flushAndClear();
        ProductEntity loadedEarlier = productRepository.findByReference(reference).orElseThrow();
        entityManager.detach(loadedEarlier);

        // Somebody else changes the product in the meantime
        ProductEntity current = productRepository.findByReference(reference).orElseThrow();
        current.setStockQuantity(49);
        flushAndClear();

        loadedEarlier.setStockQuantity(40);
        assertThatThrownBy(() -> productRepository.saveAndFlush(loadedEarlier))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void rejectsSameNameInCategoryIgnoringCase() {
        productRepository.save(product(UUID.randomUUID(), "Moon dust catnip", "7.25", 50, toys));

        assertThatThrownBy(() -> productRepository.saveAndFlush(
                product(UUID.randomUUID(), "MOON DUST CATNIP", "1.00", 1, toys)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nameCheckFindsNameOfAnotherProductIgnoringCase() {
        saveCatnip();

        assertThat(productRepository.existsByNameInCategory(toys.getReference(), "MOON dust CATNIP", UUID.randomUUID()))
                .isTrue();
    }

    @Test
    void nameCheckIgnoresProductItself() {
        UUID catnipId = saveCatnip();

        assertThat(productRepository.existsByNameInCategory(toys.getReference(), "Moon dust catnip", catnipId))
                .isFalse();
    }

    @Test
    void nameCheckIgnoresOtherCategories() {
        saveCatnip();

        assertThat(productRepository.existsByNameInCategory(food.getReference(), "Moon dust catnip", UUID.randomUUID()))
                .isFalse();
    }

    @Test
    void pagesGoThroughAllProductsInTheSameOrderAsOneQuery() {
        // Same name with different case: the order between them is decided by reference
        productRepository.save(product(UUID.randomUUID(), "Star ball", "1.00", 1, toys));
        productRepository.save(product(UUID.randomUUID(), "STAR BALL", "1.00", 1, food));
        productRepository.save(product(UUID.randomUUID(), "asteroid chew toy", "1.00", 1, toys));
        productRepository.save(product(UUID.randomUUID(), "Zero-g space mouse", "1.00", 1, food));
        flushAndClear();

        List<UUID> allAtOnce = references(productRepository.findFirstPage(Limit.unlimited()));
        List<UUID> pageByPage = new ArrayList<>();
        List<ProductEntity> page = productRepository.findFirstPage(Limit.of(2));
        while (!page.isEmpty()) {
            assertThat(page).hasSizeLessThanOrEqualTo(2);
            pageByPage.addAll(references(page));
            ProductEntity last = page.get(page.size() - 1);
            page = productRepository.findPageAfter(last.getName(), last.getReference(), Limit.of(2));
        }

        assertThat(pageByPage).isEqualTo(allAtOnce).doesNotHaveDuplicates();
        assertThat(productRepository.findFirstPage(Limit.unlimited()))
                .extracting(ProductEntity::getName)
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER)
                .containsSubsequence("asteroid chew toy", "Galaxy cosmic milk", "Zero-g space mouse");
    }

    @Test
    void returnsCategoryWithProductsWithoutExtraQueries() {
        productRepository.save(product(UUID.randomUUID(), "Star ball", "1.00", 1, toys));
        flushAndClear();

        assertThat(productRepository.findFirstPage(Limit.of(100)))
                .allSatisfy(product -> assertThat(Hibernate.isInitialized(product.getCategory())).isTrue());
    }

    @Test
    void findsProductsOfCategorySortedByName() {
        productRepository.save(product(UUID.randomUUID(), "Nebula ribbon", "1.00", 1, toys));
        productRepository.save(product(UUID.randomUUID(), "Comet feather", "1.00", 1, toys));
        productRepository.save(product(UUID.randomUUID(), "Galaxy bowl", "1.00", 1, food));
        flushAndClear();

        assertThat(productRepository.findByCategoryReferenceOrderByNameAscReferenceAsc(toys.getReference()))
                .extracting(ProductEntity::getName)
                .containsExactly("Comet feather", "Nebula ribbon");
    }

    private UUID saveCatnip() {
        UUID catnipId = UUID.randomUUID();
        productRepository.save(product(catnipId, "Moon dust catnip", "7.25", 50, toys));
        flushAndClear();
        return catnipId;
    }

    private static List<UUID> references(List<ProductEntity> products) {
        return products.stream().map(ProductEntity::getReference).toList();
    }
}

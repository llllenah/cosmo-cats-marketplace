package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.application.product.PageCursor;
import com.cosmocats.marketplace.domain.cat.CosmoCat;
import com.cosmocats.marketplace.domain.category.Category;
import com.cosmocats.marketplace.domain.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

@DisplayName("In-memory repositories")
class InMemoryRepositoriesTest {

    private static final UUID YARN_BALL_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    @Nested
    @DisplayName("InMemoryProductRepository")
    class ProductRepositoryTest {

        private final InMemoryProductRepository repository = new InMemoryProductRepository();

        @Test
        void startsWithMockCatalog() {
            assertThat(repository.count()).isEqualTo(4);
            assertThat(repository.findById(YARN_BALL_ID)).get()
                    .extracting(Product::name).isEqualTo("Anti-gravity star yarn ball");
        }

        @Test
        void savesNewProductAndReplacesExistingOne() {
            Product product = product("Moon dust catnip");

            repository.save(product);
            repository.save(new Product(product.id(), "Moon dust catnip XL", null, BigDecimal.TEN, 1,
                    MockCatalogData.COSMIC_FOOD_ID));

            assertThat(repository.count()).isEqualTo(5);
            assertThat(repository.findById(product.id())).get()
                    .extracting(Product::name).isEqualTo("Moon dust catnip XL");
        }

        @Test
        void findsProductByNameIgnoringCase() {
            assertThat(repository.findByNameIgnoreCase("GALAXY COSMIC MILK")).get()
                    .extracting(Product::name).isEqualTo("Galaxy cosmic milk");
            assertThat(repository.findByNameIgnoreCase("Galaxy milk")).isEmpty();
        }

        @Test
        void returnsProductsSortedByNameStartingAfterCursor() {
            List<String> firstPage = names(repository.findPageAfter(null, 2));
            Product lastOnFirstPage = repository.findPageAfter(null, 2).get(1);
            List<String> secondPage = names(repository.findPageAfter(PageCursor.after(lastOnFirstPage), 2));

            assertThat(firstPage).containsExactly("Anti-gravity star yarn ball", "Comet tail scratching post");
            assertThat(secondPage).containsExactly("Galaxy cosmic milk", "Nebula nap capsule");
        }

        @Test
        void pagesAreNotShiftedWhenProductIsDeletedBetweenRequests() {
            Product lastOnFirstPage = repository.findPageAfter(null, 2).get(1);

            repository.deleteById(YARN_BALL_ID);

            assertThat(names(repository.findPageAfter(PageCursor.after(lastOnFirstPage), 2)))
                    .containsExactly("Galaxy cosmic milk", "Nebula nap capsule");
        }

        @Test
        void returnsEmptyListAfterLastProduct() {
            PageCursor afterEverything = new PageCursor("zzz", UUID.randomUUID());

            assertThat(repository.findPageAfter(afterEverything, 10)).isEmpty();
        }

        @Test
        void findsProductsOfCategorySortedByName() {
            repository.save(new Product(UUID.randomUUID(), "Asteroid chew toy", null, BigDecimal.ONE, 1,
                    MockCatalogData.SPACE_GEAR_ID));

            assertThat(names(repository.findByCategoryId(MockCatalogData.SPACE_GEAR_ID)))
                    .containsExactly("Asteroid chew toy", "Comet tail scratching post", "Nebula nap capsule");
            assertThat(repository.findByCategoryId(UUID.randomUUID())).isEmpty();
        }

        @Test
        void deletesProduct() {
            assertThat(repository.existsById(YARN_BALL_ID)).isTrue();

            repository.deleteById(YARN_BALL_ID);

            assertThat(repository.existsById(YARN_BALL_ID)).isFalse();
            assertThat(repository.findById(YARN_BALL_ID)).isEmpty();
            assertThat(repository.count()).isEqualTo(3);
        }

        @Test
        void savesProductsFromManyThreadsWithoutLosingAny() throws InterruptedException {
            List<Thread> threads = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                int number = i;
                threads.add(new Thread(() -> repository.save(product("Star ball " + number))));
            }
            threads.forEach(Thread::start);
            for (Thread thread : threads) {
                thread.join();
            }

            assertThat(repository.count()).isEqualTo(54);
        }

        private static List<String> names(List<Product> products) {
            return products.stream().map(Product::name).toList();
        }

        private static Product product(String name) {
            return new Product(UUID.randomUUID(), name, null, BigDecimal.ONE, 1, MockCatalogData.COSMIC_FOOD_ID);
        }
    }

    @Nested
    @DisplayName("InMemoryCategoryRepository")
    class CategoryRepositoryTest {

        private final InMemoryCategoryRepository repository = new InMemoryCategoryRepository();

        @Test
        void containsMockCategories() {
            assertThat(repository.findAll()).extracting(Category::name)
                    .containsExactlyInAnyOrder("Anti-gravity toys", "Cosmic food", "Space gear");
        }

        @Test
        void findsCategoryById() {
            assertThat(repository.findById(MockCatalogData.SPACE_GEAR_ID)).get()
                    .extracting(Category::name).isEqualTo("Space gear");
            assertThat(repository.findById(UUID.randomUUID())).isEmpty();
        }

        @Test
        void checksIfCategoryExists() {
            assertThat(repository.existsById(MockCatalogData.ANTI_GRAVITY_TOYS_ID)).isTrue();
            assertThat(repository.existsById(UUID.randomUUID())).isFalse();
        }
    }

    @Nested
    @DisplayName("InMemoryCosmoCatRepository")
    class CosmoCatRepositoryTest {

        @Test
        void returnsMockCatsSortedByName() {
            assertThat(new InMemoryCosmoCatRepository().findAll())
                    .extracting(CosmoCat::name, CosmoCat::homePlanet)
                    .containsExactly(
                            tuple("Luna", "Moon"),
                            tuple("Nova", "Kepler-452b"),
                            tuple("Orion", "Mars"));
        }
    }
}

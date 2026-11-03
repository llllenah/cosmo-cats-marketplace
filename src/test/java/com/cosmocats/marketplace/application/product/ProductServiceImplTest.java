package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.application.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.application.exception.ProductInUseException;
import com.cosmocats.marketplace.application.exception.ProductNameAlreadyExistsException;
import com.cosmocats.marketplace.application.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.CategoryRepository;
import com.cosmocats.marketplace.infrastructure.persistence.EntityMapper;
import com.cosmocats.marketplace.infrastructure.persistence.OrderRepository;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import com.cosmocats.marketplace.infrastructure.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.cosmocats.marketplace.infrastructure.persistence.TestEntities.category;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductServiceImpl")
class ProductServiceImplTest {

    private static final UUID PRODUCT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final CategoryEntity toys = category(CATEGORY_ID, "Anti-gravity toys");
    private final CategoryEntity food = category(OTHER_CATEGORY_ID, "Cosmic food");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductValidationService productValidationService;

    @Spy
    private EntityMapper entityMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Captor
    private ArgumentCaptor<ProductEntity> entityCaptor;

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        void generatesNewIdValidatesAndSavesProduct() {
            when(categoryRepository.findByReference(CATEGORY_ID)).thenReturn(Optional.of(toys));
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Product created = productService.createProduct(product(null, "Star yarn ball", CATEGORY_ID));

            assertThat(created.id()).isNotNull();
            assertThat(created).isEqualTo(product(created.id(), "Star yarn ball", CATEGORY_ID));
            var callOrder = inOrder(productValidationService, productRepository);
            callOrder.verify(productValidationService).validateBeforeSave(productCaptor.capture());
            callOrder.verify(productRepository).save(entityCaptor.capture());
            assertThat(productCaptor.getValue().id()).isEqualTo(created.id());
            assertThat(entityCaptor.getValue().getReference()).isEqualTo(created.id());
            assertThat(entityCaptor.getValue().getCategory()).isSameAs(toys);
        }

        @Test
        void ignoresIdPassedByCallerAndAlwaysGeneratesNewOne() {
            when(categoryRepository.findByReference(CATEGORY_ID)).thenReturn(Optional.of(toys));
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Product first = productService.createProduct(product(PRODUCT_ID, "Star yarn ball", CATEGORY_ID));
            Product second = productService.createProduct(product(PRODUCT_ID, "Moon yarn ball", CATEGORY_ID));

            assertThat(first.id()).isNotNull().isNotEqualTo(PRODUCT_ID).isNotEqualTo(second.id());
        }

        @Test
        void doesNotSaveProductWhenValidationFails() {
            doThrow(new ProductNameAlreadyExistsException("Star yarn ball"))
                    .when(productValidationService).validateBeforeSave(any());

            assertThatThrownBy(() -> productService.createProduct(product(null, "Star yarn ball", CATEGORY_ID)))
                    .isInstanceOf(ProductNameAlreadyExistsException.class)
                    .hasMessage("Product with name 'Star yarn ball' already exists in this category.");
            verify(productRepository, never()).save(any());
        }

        @Test
        void doesNotSaveProductWhenCategoryDisappearsAfterValidation() {
            when(categoryRepository.findByReference(CATEGORY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(product(null, "Star yarn ball", CATEGORY_ID)))
                    .isInstanceOf(CategoryNotFoundException.class);
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        void returnsProductWhenItExists() {
            when(productRepository.findByReference(PRODUCT_ID)).thenReturn(Optional.of(entity(PRODUCT_ID, "Star yarn ball")));

            assertThat(productService.getProductById(PRODUCT_ID)).isEqualTo(product(PRODUCT_ID, "Star yarn ball", CATEGORY_ID));
        }

        @Test
        void throwsNotFoundWhenProductDoesNotExist() {
            when(productRepository.findByReference(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(PRODUCT_ID))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product with id '%s' was not found.", PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("getProducts")
    class GetProducts {

        @Test
        void requestsFirstPageWithOneExtraProductToFindOutIfNextPageExists() {
            when(productRepository.findFirstPage(Limit.of(3))).thenReturn(List.of());

            productService.getProducts(null, 2);

            verify(productRepository).findFirstPage(Limit.of(3));
            verify(productRepository, never()).findPageAfter(any(), any(), any());
        }

        @Test
        void returnsFullPageWithCursorToLastProductWhenMoreProductsExist() {
            ProductEntity first = entity(UUID.randomUUID(), "Comet ball");
            ProductEntity second = entity(UUID.randomUUID(), "Moon ball");
            ProductEntity third = entity(UUID.randomUUID(), "Star ball");
            when(productRepository.findFirstPage(Limit.of(3))).thenReturn(List.of(first, second, third));
            when(productRepository.count()).thenReturn(5L);

            PagedResult<Product> page = productService.getProducts(null, 2);

            assertThat(page.content()).extracting(Product::name).containsExactly("Comet ball", "Moon ball");
            assertThat(page.size()).isEqualTo(2);
            assertThat(page.totalElements()).isEqualTo(5);
            assertThat(page.nextCursor()).isEqualTo(new PageCursor("Moon ball", second.getReference()));
        }

        @Test
        void continuesAfterCursorAndReturnsLastPageWithoutCursor() {
            PageCursor cursor = new PageCursor("Comet ball", UUID.randomUUID());
            when(productRepository.findPageAfter("Comet ball", cursor.id(), Limit.of(3)))
                    .thenReturn(List.of(entity(UUID.randomUUID(), "Star ball")));
            when(productRepository.count()).thenReturn(3L);

            PagedResult<Product> page = productService.getProducts(cursor, 2);

            assertThat(page.content()).extracting(Product::name).containsExactly("Star ball");
            assertThat(page.nextCursor()).isNull();
        }

        @Test
        void returnsPageWithoutCursorWhenProductsFitExactly() {
            when(productRepository.findFirstPage(Limit.of(3)))
                    .thenReturn(List.of(entity(UUID.randomUUID(), "Comet ball"), entity(UUID.randomUUID(), "Moon ball")));
            when(productRepository.count()).thenReturn(2L);

            PagedResult<Product> page = productService.getProducts(null, 2);

            assertThat(page.content()).hasSize(2);
            assertThat(page.nextCursor()).isNull();
        }

        @Test
        void returnsEmptyPageForEmptyCatalog() {
            when(productRepository.findFirstPage(Limit.of(21))).thenReturn(List.of());
            when(productRepository.count()).thenReturn(0L);

            PagedResult<Product> page = productService.getProducts(null, 20);

            assertThat(page.content()).isEmpty();
            assertThat(page.totalElements()).isZero();
            assertThat(page.nextCursor()).isNull();
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        void validatesAndChangesAllFieldsOfManagedEntity() {
            ProductEntity existing = entity(PRODUCT_ID, "Star yarn ball");
            Product changed = new Product(PRODUCT_ID, "Galaxy yarn ball", null, new BigDecimal("1.50"), 3, OTHER_CATEGORY_ID);
            when(productRepository.findByReference(PRODUCT_ID)).thenReturn(Optional.of(existing));
            when(categoryRepository.findByReference(OTHER_CATEGORY_ID)).thenReturn(Optional.of(food));

            Product updated = productService.updateProduct(changed);

            assertThat(updated).isEqualTo(changed);
            assertThat(existing.getName()).isEqualTo("Galaxy yarn ball");
            assertThat(existing.getDescription()).isNull();
            assertThat(existing.getPrice()).isEqualByComparingTo("1.50");
            assertThat(existing.getStockQuantity()).isEqualTo(3);
            assertThat(existing.getCategory()).isSameAs(food);
            verify(productValidationService).validateBeforeSave(changed);
            // Dirty checking writes the changes on commit, an explicit save is not needed
            verify(productRepository, never()).save(any());
        }

        @Test
        void throwsNotFoundWhenProductDoesNotExist() {
            when(productRepository.findByReference(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(product(PRODUCT_ID, "Star yarn ball", CATEGORY_ID)))
                    .isInstanceOf(ProductNotFoundException.class);
            verify(productValidationService, never()).validateBeforeSave(any());
        }

        @Test
        void doesNotChangeEntityWhenValidationFails() {
            ProductEntity existing = entity(PRODUCT_ID, "Star yarn ball");
            when(productRepository.findByReference(PRODUCT_ID)).thenReturn(Optional.of(existing));
            doThrow(new ProductNameAlreadyExistsException("Galaxy cosmic milk"))
                    .when(productValidationService).validateBeforeSave(any());

            assertThatThrownBy(() -> productService.updateProduct(product(PRODUCT_ID, "Galaxy cosmic milk", CATEGORY_ID)))
                    .isInstanceOf(ProductNameAlreadyExistsException.class);
            assertThat(existing.getName()).isEqualTo("Star yarn ball");
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        void deletesProductThatIsNotOrdered() {
            ProductEntity existing = entity(PRODUCT_ID, "Star yarn ball");
            when(productRepository.findByReference(PRODUCT_ID)).thenReturn(Optional.of(existing));
            when(orderRepository.existsByItemsProductReference(PRODUCT_ID)).thenReturn(false);

            productService.deleteProduct(PRODUCT_ID);

            verify(productRepository).delete(existing);
        }

        @Test
        void throwsNotFoundWhenProductDoesNotExist() {
            when(productRepository.findByReference(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(PRODUCT_ID))
                    .isInstanceOf(ProductNotFoundException.class);
            verify(productRepository, never()).delete(any());
        }

        @Test
        void keepsProductThatIsPartOfOrders() {
            when(productRepository.findByReference(PRODUCT_ID)).thenReturn(Optional.of(entity(PRODUCT_ID, "Star yarn ball")));
            when(orderRepository.existsByItemsProductReference(PRODUCT_ID)).thenReturn(true);

            assertThatThrownBy(() -> productService.deleteProduct(PRODUCT_ID))
                    .isInstanceOf(ProductInUseException.class)
                    .hasMessage("Product '%s' is part of existing orders and cannot be deleted.", PRODUCT_ID);
            verify(productRepository, never()).delete(any());
        }
    }

    private ProductEntity entity(UUID reference, String name) {
        ProductEntity entity = new ProductEntity();
        entity.setReference(reference);
        entity.setName(name);
        entity.setDescription("Description");
        entity.setPrice(new BigDecimal("9.99"));
        entity.setStockQuantity(10);
        entity.setCategory(toys);
        return entity;
    }

    private static Product product(UUID id, String name, UUID categoryId) {
        return new Product(id, name, "Description", new BigDecimal("9.99"), 10, categoryId);
    }
}

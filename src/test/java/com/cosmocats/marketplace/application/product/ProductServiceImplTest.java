package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.application.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.application.exception.ProductNameAlreadyExistsException;
import com.cosmocats.marketplace.application.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductValidationService productValidationService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        void generatesNewIdValidatesAndSavesProduct() {
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Product created = productService.createProduct(product(null, "Star yarn ball"));

            assertThat(created.id()).isNotNull();
            assertThat(created.name()).isEqualTo("Star yarn ball");
            var callOrder = inOrder(productValidationService, productRepository);
            callOrder.verify(productValidationService).validateBeforeSave(productCaptor.capture());
            callOrder.verify(productRepository).save(created);
            assertThat(productCaptor.getValue().id()).isEqualTo(created.id());
        }

        @Test
        void ignoresIdPassedByCallerAndAlwaysGeneratesNewOne() {
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Product created = productService.createProduct(product(PRODUCT_ID, "Star yarn ball"));

            assertThat(created.id()).isNotNull().isNotEqualTo(PRODUCT_ID);
        }

        @Test
        void generatesDifferentIdsForEveryProduct() {
            when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Product first = productService.createProduct(product(null, "Star yarn ball"));
            Product second = productService.createProduct(product(null, "Moon yarn ball"));

            assertThat(first.id()).isNotEqualTo(second.id());
        }

        @Test
        void doesNotSaveProductWhenCategoryDoesNotExist() {
            doThrow(new CategoryNotFoundException(CATEGORY_ID))
                    .when(productValidationService).validateBeforeSave(any());

            assertThatThrownBy(() -> productService.createProduct(product(null, "Star yarn ball")))
                    .isInstanceOf(CategoryNotFoundException.class);
            verify(productRepository, never()).save(any());
        }

        @Test
        void doesNotSaveProductWhenNameIsTaken() {
            doThrow(new ProductNameAlreadyExistsException("Star yarn ball"))
                    .when(productValidationService).validateBeforeSave(any());

            assertThatThrownBy(() -> productService.createProduct(product(null, "Star yarn ball")))
                    .isInstanceOf(ProductNameAlreadyExistsException.class)
                    .hasMessage("Product with name 'Star yarn ball' already exists.");
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getProductById")
    class GetProductById {

        @Test
        void returnsProductWhenItExists() {
            Product existing = product(PRODUCT_ID, "Star yarn ball");
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existing));

            assertThat(productService.getProductById(PRODUCT_ID)).isEqualTo(existing);
        }

        @Test
        void throwsNotFoundWhenProductDoesNotExist() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProductById(PRODUCT_ID))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessage("Product with id '%s' was not found.", PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("getProducts")
    class GetProducts {

        @Test
        void requestsOneExtraProductToFindOutIfNextPageExists() {
            when(productRepository.findPageAfter(null, 3)).thenReturn(List.of());

            productService.getProducts(null, 2);

            verify(productRepository).findPageAfter(null, 3);
        }

        @Test
        void returnsFullPageWithCursorToLastProductWhenMoreProductsExist() {
            Product first = product(UUID.randomUUID(), "Comet ball");
            Product second = product(UUID.randomUUID(), "Moon ball");
            Product third = product(UUID.randomUUID(), "Star ball");
            when(productRepository.findPageAfter(null, 3)).thenReturn(List.of(first, second, third));
            when(productRepository.count()).thenReturn(5L);

            PagedResult<Product> page = productService.getProducts(null, 2);

            assertThat(page.content()).containsExactly(first, second);
            assertThat(page.size()).isEqualTo(2);
            assertThat(page.totalElements()).isEqualTo(5);
            assertThat(page.nextCursor()).isEqualTo(new PageCursor("Moon ball", second.id()));
        }

        @Test
        void returnsLastPageWithoutCursor() {
            PageCursor cursor = new PageCursor("Comet ball", UUID.randomUUID());
            Product last = product(UUID.randomUUID(), "Star ball");
            when(productRepository.findPageAfter(cursor, 3)).thenReturn(List.of(last));
            when(productRepository.count()).thenReturn(3L);

            PagedResult<Product> page = productService.getProducts(cursor, 2);

            assertThat(page.content()).containsExactly(last);
            assertThat(page.nextCursor()).isNull();
        }

        @Test
        void returnsPageWithoutCursorWhenProductsFitExactly() {
            Product first = product(UUID.randomUUID(), "Comet ball");
            Product second = product(UUID.randomUUID(), "Moon ball");
            when(productRepository.findPageAfter(null, 3)).thenReturn(List.of(first, second));
            when(productRepository.count()).thenReturn(2L);

            PagedResult<Product> page = productService.getProducts(null, 2);

            assertThat(page.content()).containsExactly(first, second);
            assertThat(page.nextCursor()).isNull();
        }

        @Test
        void returnsEmptyPageForEmptyCatalog() {
            when(productRepository.findPageAfter(any(), anyInt())).thenReturn(List.of());
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
        void validatesAndSavesExistingProduct() {
            Product changed = product(PRODUCT_ID, "Galaxy yarn ball");
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(PRODUCT_ID, "Star yarn ball")));
            when(productRepository.save(changed)).thenReturn(changed);

            Product updated = productService.updateProduct(changed);

            assertThat(updated).isEqualTo(changed);
            var callOrder = inOrder(productValidationService, productRepository);
            callOrder.verify(productValidationService).validateBeforeSave(changed);
            callOrder.verify(productRepository).save(changed);
        }

        @Test
        void throwsNotFoundAndDoesNotCreateProductWhenItDoesNotExist() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(product(PRODUCT_ID, "Star yarn ball")))
                    .isInstanceOf(ProductNotFoundException.class);
            verify(productValidationService, never()).validateBeforeSave(any());
            verify(productRepository, never()).save(any());
        }

        @Test
        void doesNotSaveProductWhenValidationFails() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product(PRODUCT_ID, "Star yarn ball")));
            doThrow(new ProductNameAlreadyExistsException("Galaxy cosmic milk"))
                    .when(productValidationService).validateBeforeSave(any());

            assertThatThrownBy(() -> productService.updateProduct(product(PRODUCT_ID, "Galaxy cosmic milk")))
                    .isInstanceOf(ProductNameAlreadyExistsException.class);
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        void deletesExistingProduct() {
            when(productRepository.existsById(PRODUCT_ID)).thenReturn(true);

            productService.deleteProduct(PRODUCT_ID);

            verify(productRepository).deleteById(PRODUCT_ID);
        }

        @Test
        void throwsNotFoundWhenProductDoesNotExist() {
            when(productRepository.existsById(PRODUCT_ID)).thenReturn(false);

            assertThatThrownBy(() -> productService.deleteProduct(PRODUCT_ID))
                    .isInstanceOf(ProductNotFoundException.class);
            verify(productRepository, never()).deleteById(any());
        }
    }

    private static Product product(UUID id, String name) {
        return new Product(id, name, "Description", new BigDecimal("9.99"), 10, CATEGORY_ID);
    }
}

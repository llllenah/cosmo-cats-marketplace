package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.application.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.application.exception.ProductNameAlreadyExistsException;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.CategoryRepository;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductValidationService")
class ProductValidationServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID OTHER_PRODUCT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductValidationService productValidationService;

    @Test
    void acceptsProductWithExistingCategoryAndFreeName() {
        when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(true);
        when(productRepository.findByNameIgnoreCase("Star yarn ball")).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() ->
                productValidationService.validateBeforeSave(product(PRODUCT_ID, "Star yarn ball")));
    }

    @Test
    void rejectsProductWithUnknownCategoryBeforeCheckingName() {
        when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(false);

        assertThatThrownBy(() -> productValidationService.validateBeforeSave(product(PRODUCT_ID, "Star yarn ball")))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category with id '%s' does not exist.", CATEGORY_ID);
        verify(productRepository, never()).findByNameIgnoreCase(any());
    }

    @Test
    void rejectsNameThatBelongsToAnotherProduct() {
        when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(true);
        when(productRepository.findByNameIgnoreCase("galaxy COSMIC milk"))
                .thenReturn(Optional.of(product(OTHER_PRODUCT_ID, "Galaxy cosmic milk")));

        assertThatThrownBy(() -> productValidationService.validateBeforeSave(product(PRODUCT_ID, "galaxy COSMIC milk")))
                .isInstanceOf(ProductNameAlreadyExistsException.class)
                .hasMessage("Product with name 'galaxy COSMIC milk' already exists.");
    }

    @Test
    void allowsProductToKeepItsOwnNameOnUpdate() {
        when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(true);
        when(productRepository.findByNameIgnoreCase("Star yarn ball"))
                .thenReturn(Optional.of(product(PRODUCT_ID, "Star yarn ball")));

        assertThatNoException().isThrownBy(() ->
                productValidationService.validateBeforeSave(product(PRODUCT_ID, "Star yarn ball")));
    }

    @Test
    void categoryExistsDelegatesToCategoryRepository() {
        UUID unknownCategoryId = UUID.randomUUID();
        when(categoryRepository.existsById(CATEGORY_ID)).thenReturn(true);
        when(categoryRepository.existsById(unknownCategoryId)).thenReturn(false);

        assertThat(productValidationService.categoryExists(CATEGORY_ID)).isTrue();
        assertThat(productValidationService.categoryExists(unknownCategoryId)).isFalse();
    }

    private static Product product(UUID id, String name) {
        return new Product(id, name, null, new BigDecimal("9.99"), 1, CATEGORY_ID);
    }
}

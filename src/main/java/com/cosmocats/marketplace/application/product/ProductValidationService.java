package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.application.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.application.exception.ProductNameAlreadyExistsException;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.CategoryRepository;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business validations that need data from repositories.
 * Basic field rules are checked earlier by Bean Validation and ProductMapper.
 * The database has the same rules as constraints (uq_product_category_name, fk_product_category),
 * the checks here give a clear error instead of a constraint violation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductValidationService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Checks a product that already has its id (new or existing) before it is saved.
     * The name must be unique within the category, ignoring case.
     */
    public void validateBeforeSave(Product product) {
        if (!categoryExists(product.categoryId())) {
            throw new CategoryNotFoundException(product.categoryId());
        }
        if (productRepository.existsByNameInCategory(product.categoryId(), product.name(), product.id())) {
            throw new ProductNameAlreadyExistsException(product.name());
        }
    }

    public boolean categoryExists(UUID categoryId) {
        return categoryRepository.existsByReference(categoryId);
    }
}

package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.application.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.application.exception.ProductNameAlreadyExistsException;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.CategoryRepository;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Business validations that need data from repositories.
 * Basic field rules are checked earlier by Bean Validation and ProductMapper.
 */
@Service
@RequiredArgsConstructor
public class ProductValidationService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Checks a product that already has its id (new or existing) before it is saved.
     */
    public void validateBeforeSave(Product product) {
        if (!categoryExists(product.categoryId())) {
            throw new CategoryNotFoundException(product.categoryId());
        }
        productRepository.findByNameIgnoreCase(product.name())
                .filter(existing -> !existing.id().equals(product.id()))
                .ifPresent(existing -> {
                    throw new ProductNameAlreadyExistsException(product.name());
                });
    }

    public boolean categoryExists(UUID categoryId) {
        return categoryRepository.existsById(categoryId);
    }
}

package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.application.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.application.exception.ProductNameAlreadyExistsException;
import com.cosmocats.marketplace.application.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.category.CategoryRepository;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.domain.product.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductServiceImpl(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Product createProduct(Product product) {
        ensureCategoryExists(product.categoryId());
        if (productRepository.existsByNameIgnoreCase(product.name())) {
            throw new ProductNameAlreadyExistsException(product.name());
        }
        return productRepository.save(product.withId(UUID.randomUUID()));
    }

    @Override
    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public PagedResult<Product> getProducts(int page, int size) {
        return new PagedResult<>(productRepository.findPage(page, size), page, size, productRepository.count());
    }

    @Override
    public Product updateProduct(UUID id, Product product) {
        Product existingProduct = getProductById(id);
        ensureCategoryExists(product.categoryId());
        boolean nameChanged = !existingProduct.name().equalsIgnoreCase(product.name());
        if (nameChanged && productRepository.existsByNameIgnoreCase(product.name())) {
            throw new ProductNameAlreadyExistsException(product.name());
        }
        return productRepository.save(product.withId(id));
    }

    @Override
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    private void ensureCategoryExists(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }
    }
}

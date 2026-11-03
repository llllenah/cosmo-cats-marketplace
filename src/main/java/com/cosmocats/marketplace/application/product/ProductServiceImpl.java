package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.application.exception.CategoryNotFoundException;
import com.cosmocats.marketplace.application.exception.ProductInUseException;
import com.cosmocats.marketplace.application.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.CategoryRepository;
import com.cosmocats.marketplace.infrastructure.persistence.EntityMapper;
import com.cosmocats.marketplace.infrastructure.persistence.OrderRepository;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Read methods run in read-only transactions (Hibernate skips dirty checking for them),
 * every write method is one transaction: validation and saving see the same data.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OrderRepository orderRepository;
    private final ProductValidationService productValidationService;
    private final EntityMapper entityMapper;

    @Override
    @Transactional
    public Product createProduct(Product product) {
        Product newProduct = product.withId(UUID.randomUUID());
        productValidationService.validateBeforeSave(newProduct);
        ProductEntity entity = new ProductEntity();
        entity.setReference(newProduct.id());
        copyFields(newProduct, entity);
        return entityMapper.toDomain(productRepository.save(entity));
    }

    @Override
    public Product getProductById(UUID id) {
        return entityMapper.toDomain(findProduct(id));
    }

    @Override
    public PagedResult<Product> getProducts(PageCursor cursor, int size) {
        // One extra product tells us whether a next page exists.
        Limit limit = Limit.of(size + 1);
        List<Product> found = (cursor == null
                ? productRepository.findFirstPage(limit)
                : productRepository.findPageAfter(cursor.name(), cursor.id(), limit))
                .stream()
                .map(entityMapper::toDomain)
                .toList();
        boolean hasNext = found.size() > size;
        List<Product> page = hasNext ? found.subList(0, size) : found;
        PageCursor nextCursor = hasNext ? PageCursor.after(page.get(page.size() - 1)) : null;
        return new PagedResult<>(page, size, productRepository.count(), nextCursor);
    }

    @Override
    @Transactional
    public Product updateProduct(Product product) {
        ProductEntity entity = findProduct(product.id());
        productValidationService.validateBeforeSave(product);
        copyFields(product, entity);
        // The entity is managed, Hibernate writes the changes when the transaction commits
        return entityMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        ProductEntity entity = findProduct(id);
        if (orderRepository.existsByItemsProductReference(id)) {
            throw new ProductInUseException(id);
        }
        productRepository.delete(entity);
    }

    private ProductEntity findProduct(UUID id) {
        return productRepository.findByReference(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private void copyFields(Product product, ProductEntity entity) {
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setPrice(product.price());
        entity.setStockQuantity(product.stockQuantity());
        entity.setCategory(categoryRepository.findByReference(product.categoryId())
                .orElseThrow(() -> new CategoryNotFoundException(product.categoryId())));
    }
}

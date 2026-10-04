package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.application.exception.ProductNotFoundException;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.persistence.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductValidationService productValidationService;

    @Override
    public Product createProduct(Product product) {
        Product newProduct = product.withId(UUID.randomUUID());
        productValidationService.validateBeforeSave(newProduct);
        return productRepository.save(newProduct);
    }

    @Override
    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public PagedResult<Product> getProducts(PageCursor cursor, int size) {
        // One extra product tells us whether a next page exists.
        List<Product> found = productRepository.findPageAfter(cursor, size + 1);
        boolean hasNext = found.size() > size;
        List<Product> page = hasNext ? found.subList(0, size) : found;
        PageCursor nextCursor = hasNext ? PageCursor.after(page.get(page.size() - 1)) : null;
        return new PagedResult<>(page, size, productRepository.count(), nextCursor);
    }

    @Override
    public Product updateProduct(Product product) {
        getProductById(product.id());
        productValidationService.validateBeforeSave(product);
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}

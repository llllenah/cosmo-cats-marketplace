package com.cosmocats.marketplace.application.product;

import com.cosmocats.marketplace.domain.product.Product;

import java.util.UUID;

public interface ProductService {

    Product createProduct(Product product);

    Product getProductById(UUID id);

    PagedResult<Product> getProducts(PageCursor cursor, int size);

    Product updateProduct(Product product);

    void deleteProduct(UUID id);
}

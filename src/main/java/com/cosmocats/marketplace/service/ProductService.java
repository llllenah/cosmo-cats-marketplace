package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.model.Product;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {

    private Map<Long, Product> products = new HashMap<>();
    private long counter = 1;

    public ProductService() {
        products.put(counter, new Product(counter++, "Anti-gravity yarn ball", "Floats in the air", 12.5, 120, "toys"));
        products.put(counter, new Product(counter++, "Cosmic milk", "Milk from the Milky Way", 4.99, 300, "food"));
        products.put(counter, new Product(counter++, "Comet scratcher", "Sparkles", 39.0, 25, "gear"));
    }

    public List<Product> getAll(int page, int size) {
        List<Product> all = new ArrayList<>(products.values());
        return all.subList(page * size, page * size + size);
    }

    public Product getById(Long id) {
        Product product = products.get(id);
        if (product == null) {
            throw new RuntimeException("not found");
        }
        return product;
    }

    public Product create(Product product) {
        if (product.id == null) {
            product.id = counter++;
        }
        products.put(product.id, product);
        return product;
    }

    public Product update(Product product) {
        products.put(product.id, product);
        return product;
    }

    public void delete(Long id) {
        products.remove(id);
    }
}

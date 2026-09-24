package com.cosmocats.marketplace.model;

import java.util.ArrayList;
import java.util.List;

public class Cart {

    public Long id;
    public Long userId;
    public List<Product> products = new ArrayList<>();
    public Double total = 0.0;

    public void addProduct(Product product) {
        products.add(product);
        total = total + product.getPrice();
    }
}

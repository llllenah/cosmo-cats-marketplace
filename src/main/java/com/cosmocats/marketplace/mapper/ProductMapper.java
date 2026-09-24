package com.cosmocats.marketplace.mapper;

import com.cosmocats.marketplace.dto.ProductDto;
import com.cosmocats.marketplace.model.Product;

public class ProductMapper {

    public static ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.id = product.id;
        dto.name = product.name;
        dto.description = product.description;
        dto.price = product.price;
        dto.quantity = product.quantity;
        dto.category = product.category;
        return dto;
    }

    public static Product toDomain(ProductDto dto) {
        return new Product(dto.id, dto.name, dto.description, dto.price, dto.quantity, dto.category);
    }
}

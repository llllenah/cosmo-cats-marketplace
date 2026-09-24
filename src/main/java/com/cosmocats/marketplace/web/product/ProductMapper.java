package com.cosmocats.marketplace.web.product;

import com.cosmocats.marketplace.application.product.PagedResult;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.web.common.PageResponse;
import com.cosmocats.marketplace.web.product.dto.ProductRequestDto;
import com.cosmocats.marketplace.web.product.dto.ProductResponseDto;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDomain(ProductRequestDto request) {
        String description = request.description() == null ? null : request.description().trim();
        return new Product(
                null,
                request.name().trim(),
                description,
                request.price(),
                request.stockQuantity(),
                request.categoryId()
        );
    }

    public ProductResponseDto toDto(Product product) {
        return new ProductResponseDto(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.stockQuantity(),
                product.categoryId()
        );
    }

    public PageResponse<ProductResponseDto> toPageResponse(PagedResult<Product> pagedProducts) {
        PagedResult<ProductResponseDto> pagedDtos = pagedProducts.map(this::toDto);
        return new PageResponse<>(
                pagedDtos.content(),
                pagedDtos.page(),
                pagedDtos.size(),
                pagedDtos.totalElements(),
                pagedDtos.totalPages()
        );
    }
}

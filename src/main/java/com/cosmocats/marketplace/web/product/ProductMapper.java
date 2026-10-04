package com.cosmocats.marketplace.web.product;

import com.cosmocats.marketplace.application.exception.ProductValidationException;
import com.cosmocats.marketplace.application.exception.ProductValidationException.Violation;
import com.cosmocats.marketplace.application.product.PagedResult;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.web.common.PageInfo;
import com.cosmocats.marketplace.web.common.PageResponse;
import com.cosmocats.marketplace.web.common.PageTokenCodec;
import com.cosmocats.marketplace.web.product.dto.ProductRequestDto;
import com.cosmocats.marketplace.web.product.dto.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private static final int MAX_PRICE_SCALE = 2;

    private final PageTokenCodec pageTokenCodec;

    /**
     * Maps a create request. The id is generated later by the service.
     */
    public Product toDomain(ProductRequestDto request) {
        return toDomain(null, request);
    }

    /**
     * Maps an update request. The id always comes from the URL, never from the body.
     */
    public Product toDomain(UUID id, ProductRequestDto request) {
        validate(request);
        String description = request.description() == null ? null : request.description().trim();
        return new Product(
                id,
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
        PageInfo pageInfo = new PageInfo(
                pagedProducts.size(),
                pagedProducts.totalElements(),
                pageTokenCodec.encode(pagedProducts.nextCursor())
        );
        return new PageResponse<>(pagedProducts.content().stream().map(this::toDto).toList(), pageInfo);
    }

    private void validate(ProductRequestDto request) {
        List<Violation> violations = new ArrayList<>();
        if (request.name() == null || request.name().isBlank()) {
            violations.add(new Violation("name", "must not be blank"));
        }
        BigDecimal price = request.price();
        if (price == null) {
            violations.add(new Violation("price", "must not be null"));
        } else if (price.signum() <= 0) {
            violations.add(new Violation("price", "must be greater than 0"));
        } else if (price.stripTrailingZeros().scale() > MAX_PRICE_SCALE) {
            violations.add(new Violation("price", "must have at most 2 decimal places"));
        }
        if (request.stockQuantity() == null) {
            violations.add(new Violation("stockQuantity", "must not be null"));
        } else if (request.stockQuantity() < 0) {
            violations.add(new Violation("stockQuantity", "must be greater than or equal to 0"));
        }
        if (request.categoryId() == null) {
            violations.add(new Violation("categoryId", "must not be null"));
        }
        if (!violations.isEmpty()) {
            throw new ProductValidationException(violations);
        }
    }
}

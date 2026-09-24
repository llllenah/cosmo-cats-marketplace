package com.cosmocats.marketplace.web.product;

import com.cosmocats.marketplace.application.product.ProductService;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.web.common.PageResponse;
import com.cosmocats.marketplace.web.product.dto.ProductRequestDto;
import com.cosmocats.marketplace.web.product.dto.ProductResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private static final String DEFAULT_PAGE = "0";
    private static final String DEFAULT_PAGE_SIZE = "20";
    private static final int MAX_PAGE_SIZE = 100;

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductRequestDto request) {
        Product createdProduct = productService.createProduct(productMapper.toDomain(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdProduct.id())
                .toUri();
        return ResponseEntity.created(location).body(productMapper.toDto(createdProduct));
    }

    @GetMapping
    public PageResponse<ProductResponseDto> getProducts(
            @RequestParam(defaultValue = DEFAULT_PAGE)
            @Min(value = 0, message = "must be greater than or equal to 0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
            @Min(value = 1, message = "must be greater than or equal to 1")
            @Max(value = MAX_PAGE_SIZE, message = "must be less than or equal to 100") int size) {
        return productMapper.toPageResponse(productService.getProducts(page, size));
    }

    @GetMapping("/{id}")
    public ProductResponseDto getProductById(@PathVariable UUID id) {
        return productMapper.toDto(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ProductResponseDto updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequestDto request) {
        Product updatedProduct = productService.updateProduct(id, productMapper.toDomain(request));
        return productMapper.toDto(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

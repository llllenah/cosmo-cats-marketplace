package com.cosmocats.marketplace.web.product;

import com.cosmocats.marketplace.application.product.PageCursor;
import com.cosmocats.marketplace.application.product.ProductService;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.web.common.PageQuery;
import com.cosmocats.marketplace.web.common.PageResponse;
import com.cosmocats.marketplace.web.common.PageTokenCodec;
import com.cosmocats.marketplace.web.product.dto.ProductRequestDto;
import com.cosmocats.marketplace.web.product.dto.ProductResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;
    private final PageTokenCodec pageTokenCodec;

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
    public PageResponse<ProductResponseDto> getProducts(@Valid PageQuery pageQuery) {
        PageCursor cursor = pageTokenCodec.decode(pageQuery.pageToken());
        return productMapper.toPageResponse(productService.getProducts(cursor, pageQuery.pageSize()));
    }

    @GetMapping("/{id}")
    public ProductResponseDto getProductById(@PathVariable UUID id) {
        return productMapper.toDto(productService.getProductById(id));
    }

    @PutMapping("/{id}")
    public ProductResponseDto updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequestDto request) {
        Product updatedProduct = productService.updateProduct(productMapper.toDomain(id, request));
        return productMapper.toDto(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}

package com.cosmocats.marketplace.web.product;

import com.cosmocats.marketplace.application.product.KittyProductService;
import com.cosmocats.marketplace.web.product.dto.ProductResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kitty-products")
@RequiredArgsConstructor
public class KittyProductController {

    private final KittyProductService kittyProductService;
    private final ProductMapper productMapper;

    @GetMapping
    public List<ProductResponseDto> getKittyProducts() {
        return kittyProductService.getKittyProducts().stream().map(productMapper::toDto).toList();
    }
}

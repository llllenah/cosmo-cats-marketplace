package com.cosmocats.marketplace.controller;

import com.cosmocats.marketplace.dto.ProductDto;
import com.cosmocats.marketplace.mapper.ProductMapper;
import com.cosmocats.marketplace.model.Product;
import com.cosmocats.marketplace.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("/getAll")
    public List<ProductDto> getAll(@RequestParam int page, @RequestParam int size) {
        List<ProductDto> result = new ArrayList<>();
        for (Product p : productService.getAll(page, size)) {
            result.add(ProductMapper.toDto(p));
        }
        return result;
    }

    @GetMapping("/get/{id}")
    public ProductDto get(@PathVariable Long id) {
        return ProductMapper.toDto(productService.getById(id));
    }

    @PostMapping("/create")
    public ProductDto create(@RequestBody ProductDto dto) {
        System.out.println("Creating product " + dto.name);
        Product product = productService.create(ProductMapper.toDomain(dto));
        return ProductMapper.toDto(product);
    }

    @PutMapping("/update")
    public ProductDto update(@RequestBody ProductDto dto) {
        try {
            return ProductMapper.toDto(productService.update(ProductMapper.toDomain(dto)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return "Deleted";
    }
}

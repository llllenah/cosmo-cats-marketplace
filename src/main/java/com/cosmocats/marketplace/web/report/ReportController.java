package com.cosmocats.marketplace.web.report;

import com.cosmocats.marketplace.application.report.ProductReportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ProductReportService productReportService;

    @GetMapping("/popular-products")
    public List<PopularProductResponseDto> getPopularProducts(
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "must be greater than or equal to 1")
            @Max(value = 100, message = "must be less than or equal to 100")
            int limit) {
        return productReportService.getPopularProducts(limit).stream()
                .map(product -> new PopularProductResponseDto(product.productId(), product.productName(),
                        product.totalQuantity(), product.orderCount()))
                .toList();
    }
}

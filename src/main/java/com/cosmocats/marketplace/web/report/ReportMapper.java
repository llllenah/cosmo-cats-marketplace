package com.cosmocats.marketplace.web.report;

import com.cosmocats.marketplace.application.report.PopularProduct;
import com.cosmocats.marketplace.web.report.dto.PopularProductResponseDto;
import org.springframework.stereotype.Component;

@Component
public class ReportMapper {

    public PopularProductResponseDto toDto(PopularProduct product) {
        return new PopularProductResponseDto(
                product.productId(),
                product.productName(),
                product.totalQuantity(),
                product.orderCount()
        );
    }
}

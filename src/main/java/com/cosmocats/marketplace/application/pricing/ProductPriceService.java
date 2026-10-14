package com.cosmocats.marketplace.application.pricing;

import com.cosmocats.marketplace.application.product.ProductService;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.exchangerate.ExchangeRateClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Shows product prices in other currencies using rates from the external exchange rate service.
 */
@Service
public class ProductPriceService {

    private static final int MONEY_SCALE = 2;

    private final ProductService productService;
    private final ExchangeRateClient exchangeRateClient;
    private final String baseCurrency;

    public ProductPriceService(ProductService productService,
                               ExchangeRateClient exchangeRateClient,
                               @Value("${application.pricing.base-currency}") String baseCurrency) {
        this.productService = productService;
        this.exchangeRateClient = exchangeRateClient;
        this.baseCurrency = baseCurrency;
    }

    public ProductPrice getPrice(UUID productId, String currency) {
        // Unknown product is reported before calling the external service
        Product product = productService.getProductById(productId);
        BigDecimal exchangeRate = baseCurrency.equals(currency)
                ? BigDecimal.ONE
                : exchangeRateClient.getRate(baseCurrency, currency);
        BigDecimal convertedPrice = product.price()
                .multiply(exchangeRate)
                .setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
        return new ProductPrice(product.id(), product.price(), baseCurrency, convertedPrice, currency, exchangeRate);
    }
}

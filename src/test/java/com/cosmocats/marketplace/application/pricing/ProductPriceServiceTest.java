package com.cosmocats.marketplace.application.pricing;

import com.cosmocats.marketplace.application.exception.ExchangeRateUnavailableException;
import com.cosmocats.marketplace.application.exception.ProductNotFoundException;
import com.cosmocats.marketplace.application.exception.UnsupportedCurrencyException;
import com.cosmocats.marketplace.application.product.ProductService;
import com.cosmocats.marketplace.domain.product.Product;
import com.cosmocats.marketplace.infrastructure.exchangerate.ExchangeRateClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductPriceService")
class ProductPriceServiceTest {

    private static final UUID PRODUCT_ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

    @Mock
    private ProductService productService;

    @Mock
    private ExchangeRateClient exchangeRateClient;

    private ProductPriceService productPriceService;

    @BeforeEach
    void setUp() {
        productPriceService = new ProductPriceService(productService, exchangeRateClient, "USD");
    }

    @Test
    void convertsPriceWithRateFromClient() {
        givenProductWithPrice("12.50");
        when(exchangeRateClient.getRate("USD", "EUR")).thenReturn(new BigDecimal("0.92"));

        ProductPrice price = productPriceService.getPrice(PRODUCT_ID, "EUR");

        assertThat(price).isEqualTo(new ProductPrice(PRODUCT_ID, new BigDecimal("12.50"), "USD",
                new BigDecimal("11.50"), "EUR", new BigDecimal("0.92")));
    }

    @ParameterizedTest(name = "{0} * {1} = {2}")
    @CsvSource({
            "12.50, 0.9215, 11.52",
            "0.01, 0.5, 0.00",
            "0.03, 0.5, 0.02",
            "0.05, 0.5, 0.02",
            "89.90, 41.25, 3708.38"
    })
    void roundsConvertedPriceToCentsUsingBankersRounding(String basePrice, String rate, String expected) {
        givenProductWithPrice(basePrice);
        when(exchangeRateClient.getRate("USD", "UAH")).thenReturn(new BigDecimal(rate));

        assertThat(productPriceService.getPrice(PRODUCT_ID, "UAH").price()).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void usesRateOneWithoutCallingClientForBaseCurrency() {
        givenProductWithPrice("12.50");

        ProductPrice price = productPriceService.getPrice(PRODUCT_ID, "USD");

        assertThat(price.price()).isEqualTo(new BigDecimal("12.50"));
        assertThat(price.exchangeRate()).isEqualTo(BigDecimal.ONE);
        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void doesNotCallClientWhenProductDoesNotExist() {
        when(productService.getProductById(PRODUCT_ID)).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        assertThatThrownBy(() -> productPriceService.getPrice(PRODUCT_ID, "EUR"))
                .isInstanceOf(ProductNotFoundException.class);
        verifyNoInteractions(exchangeRateClient);
    }

    @Test
    void passesClientErrorsToCaller() {
        givenProductWithPrice("12.50");
        when(exchangeRateClient.getRate(any(), any()))
                .thenThrow(new UnsupportedCurrencyException("XYZ"))
                .thenThrow(new ExchangeRateUnavailableException("timeout", null));

        assertThatThrownBy(() -> productPriceService.getPrice(PRODUCT_ID, "XYZ"))
                .isInstanceOf(UnsupportedCurrencyException.class);
        assertThatThrownBy(() -> productPriceService.getPrice(PRODUCT_ID, "EUR"))
                .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    private void givenProductWithPrice(String price) {
        when(productService.getProductById(PRODUCT_ID)).thenReturn(new Product(PRODUCT_ID, "Star yarn ball", null,
                new BigDecimal(price), 1, UUID.randomUUID()));
    }
}

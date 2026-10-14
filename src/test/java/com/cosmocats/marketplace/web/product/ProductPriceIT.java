package com.cosmocats.marketplace.web.product;

import com.cosmocats.marketplace.AbstractIT;
import com.cosmocats.marketplace.infrastructure.exchangerate.ExchangeRateProperties;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/v1/products/{id}/price against the exchange rate service stubbed with WireMock.
 * Covers the happy path and the ways the external service can fail.
 */
@DisplayName("Product price in another currency (exchange rate service stubbed with WireMock)")
class ProductPriceIT extends AbstractIT {

    // "Anti-gravity star yarn ball" from mock data, price 12.50 USD
    private static final String YARN_BALL_ID = "aaaaaaaa-0000-0000-0000-000000000001";
    private static final String USD_TO_EUR_URL = "/v1/rates/USD/EUR";

    @Autowired
    private ExchangeRateProperties exchangeRateProperties;

    @Test
    void convertsPriceWithRateFromExchangeRateService() throws Exception {
        exchangeRateService.stubFor(WireMock.get(USD_TO_EUR_URL)
                .willReturn(okJson("""
                        {"base": "USD", "target": "EUR", "rate": 0.9215}
                        """)));

        requestPrice(YARN_BALL_ID, "EUR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(YARN_BALL_ID))
                .andExpect(jsonPath("$.basePrice").value(12.50))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.exchangeRate").value(0.9215))
                // 12.50 * 0.9215 = 11.51875 -> 11.52
                .andExpect(jsonPath("$.price").value(11.52));

        exchangeRateService.verify(1, getRequestedFor(urlEqualTo(USD_TO_EUR_URL)));
    }

    @Test
    void doesNotCallExchangeRateServiceForBaseCurrency() throws Exception {
        requestPrice(YARN_BALL_ID, "USD")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(12.50))
                .andExpect(jsonPath("$.exchangeRate").value(1));

        exchangeRateService.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void returns404WithoutCallingExchangeRateServiceForUnknownProduct() throws Exception {
        requestPrice("aaaaaaaa-0000-0000-0000-000000000099", "EUR")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/product-not-found"));

        exchangeRateService.verify(0, anyRequestedFor(anyUrl()));
    }

    @ParameterizedTest(name = "currency={0}")
    @ValueSource(strings = {"eur", "EURO", "E1R", ""})
    void returns400WithoutCallingExchangeRateServiceForInvalidCurrencyCode(String currency) throws Exception {
        requestPrice(YARN_BALL_ID, currency)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/validation"))
                .andExpect(jsonPath("$.errors[0].field").value("currency"))
                .andExpect(jsonPath("$.errors[0].message").value("must be a 3-letter ISO 4217 code in upper case"));

        exchangeRateService.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void returns422WhenExchangeRateServiceDoesNotKnowCurrency() throws Exception {
        exchangeRateService.stubFor(WireMock.get("/v1/rates/USD/XYZ").willReturn(notFound()));

        requestPrice(YARN_BALL_ID, "XYZ")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/currency-not-supported"))
                .andExpect(jsonPath("$.detail").value("Currency 'XYZ' is not supported by the exchange rate service."));
    }

    @Test
    void returns503WhenExchangeRateServiceFails() throws Exception {
        stubUsdToEur(serverError().withBody("java.sql.SQLException: rates-db is down"));

        expectExchangeRateUnavailable()
                .andExpect(content().string(not(containsString("rates-db"))));
    }

    @Test
    void returns503WhenExchangeRateServiceIsSlowerThanReadTimeout() throws Exception {
        // The real timeout from application.yml is used: a shorter one made the first call
        // to a cold WireMock server time out now and then
        int delayMs = (int) exchangeRateProperties.readTimeout().toMillis() + 1000;
        stubUsdToEur(okJson("""
                {"base": "USD", "target": "EUR", "rate": 0.92}
                """).withFixedDelay(delayMs));

        expectExchangeRateUnavailable();
    }

    @Test
    void returns503WhenConnectionIsDropped() throws Exception {
        stubUsdToEur(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER));

        expectExchangeRateUnavailable();
    }

    @Test
    void returns503WhenResponseIsNotJson() throws Exception {
        stubUsdToEur(aResponse().withStatus(200)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .withBody("<html>Gateway page</html>"));

        expectExchangeRateUnavailable();
    }

    @ParameterizedTest(name = "body={0}")
    @ValueSource(strings = {
            "{\"base\": \"USD\", \"target\": \"EUR\"}",
            "{\"base\": \"USD\", \"target\": \"EUR\", \"rate\": 0}",
            "{\"base\": \"USD\", \"target\": \"EUR\", \"rate\": -0.92}"
    })
    void returns503WhenRateIsMissingOrNotPositive(String body) throws Exception {
        stubUsdToEur(okJson(body));

        expectExchangeRateUnavailable();
    }

    @Test
    void returns503WhenExchangeRateServiceRejectsRequest() throws Exception {
        stubUsdToEur(aResponse().withStatus(429));

        expectExchangeRateUnavailable();
    }

    private void stubUsdToEur(ResponseDefinitionBuilder response) {
        exchangeRateService.stubFor(WireMock.get(USD_TO_EUR_URL).willReturn(response));
    }

    private ResultActions expectExchangeRateUnavailable() throws Exception {
        return requestPrice(YARN_BALL_ID, "EUR")
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/exchange-rate-unavailable"))
                .andExpect(jsonPath("$.detail").value("Exchange rates are temporarily unavailable. Please try again later."));
    }

    private ResultActions requestPrice(String productId, String currency) throws Exception {
        return mockMvc.perform(get("/api/v1/products/{id}/price", productId).param("currency", currency));
    }
}

package com.cosmocats.marketplace.application.exception;

public class UnsupportedCurrencyException extends RuntimeException {

    public UnsupportedCurrencyException(String currency) {
        super("Currency '%s' is not supported by the exchange rate service.".formatted(currency));
    }
}

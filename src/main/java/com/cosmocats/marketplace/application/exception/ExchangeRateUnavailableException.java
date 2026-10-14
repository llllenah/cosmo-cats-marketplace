package com.cosmocats.marketplace.application.exception;

/**
 * The external exchange rate service failed, timed out or returned an unusable answer.
 * The original reason is kept as the cause for logs only, it is never shown to clients.
 */
public class ExchangeRateUnavailableException extends RuntimeException {

    public ExchangeRateUnavailableException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

package com.cosmocats.marketplace.web.common;

public class InvalidPageTokenException extends RuntimeException {

    public InvalidPageTokenException() {
        super("Page token is invalid. Request the first page without pageToken.");
    }
}

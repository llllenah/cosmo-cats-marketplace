package com.cosmocats.marketplace.web.common;

import com.cosmocats.marketplace.application.product.PageCursor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Converts a page cursor to an opaque URL-safe token and back.
 * Clients must not parse the token, they only pass it to the next request.
 */
@Component
public class PageTokenCodec {

    private static final char SEPARATOR = ':';

    public String encode(PageCursor cursor) {
        if (cursor == null) {
            return null;
        }
        String raw = cursor.id().toString() + SEPARATOR + cursor.name();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public PageCursor decode(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            int separatorIndex = raw.indexOf(SEPARATOR);
            if (separatorIndex < 0) {
                throw new InvalidPageTokenException();
            }
            UUID id = UUID.fromString(raw.substring(0, separatorIndex));
            return new PageCursor(raw.substring(separatorIndex + 1), id);
        } catch (IllegalArgumentException ex) {
            throw new InvalidPageTokenException();
        }
    }
}

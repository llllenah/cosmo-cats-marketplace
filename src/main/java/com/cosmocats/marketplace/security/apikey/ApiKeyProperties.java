package com.cosmocats.marketplace.security.apikey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;
import java.util.Set;

/**
 * Clients that authenticate with an API key, by client name: {@code application.security.api-keys.<client>}.
 */
@Validated
@ConfigurationProperties("application.security")
public record ApiKeyProperties(@Valid Map<String, Client> apiKeys) {

    public ApiKeyProperties {
        apiKeys = apiKeys == null ? Map.of() : Map.copyOf(apiKeys);
    }

    /**
     * @param keySha256 SHA-256 of the key in hex; empty means the client has no key and cannot log in
     * @param roles     roles of the client without the ROLE_ prefix
     */
    public record Client(
            @Pattern(regexp = "|[0-9a-fA-F]{64}", message = "must be empty or a SHA-256 hash in hex (64 characters)")
            String keySha256,
            Set<String> roles
    ) {

        public Client {
            keySha256 = keySha256 == null ? "" : keySha256;
            roles = roles == null ? Set.of() : Set.copyOf(roles);
        }
    }
}

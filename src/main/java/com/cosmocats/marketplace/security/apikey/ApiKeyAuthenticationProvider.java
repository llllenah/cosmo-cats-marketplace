package com.cosmocats.marketplace.security.apikey;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Checks an API key against the configured clients. The configuration has only SHA-256 hashes
 * of the keys, so the key from the request is hashed and the hashes are compared.
 */
@Slf4j
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {

    // Longer values are rejected without hashing: nobody issues keys of this size
    private static final int MAX_API_KEY_LENGTH = 256;

    private final List<RegisteredClient> clients = new ArrayList<>();

    public ApiKeyAuthenticationProvider(ApiKeyProperties properties) {
        properties.apiKeys().forEach((clientName, client) -> {
            if (client.keySha256().isEmpty()) {
                log.info("API key client '{}' has no key configured and cannot log in", clientName);
                return;
            }
            List<GrantedAuthority> authorities = client.roles().stream()
                    .sorted()
                    .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            clients.add(new RegisteredClient(clientName, HexFormat.of().parseHex(client.keySha256()), authorities));
        });
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String apiKey = (String) authentication.getCredentials();
        if (apiKey == null || apiKey.isBlank() || apiKey.length() > MAX_API_KEY_LENGTH) {
            throw new BadCredentialsException("Invalid API key");
        }
        byte[] presentedHash = sha256(apiKey);
        return clients.stream()
                // MessageDigest.isEqual takes the same time wherever the first different byte is
                .filter(client -> MessageDigest.isEqual(client.keyHash(), presentedHash))
                .findFirst()
                .map(client -> ApiKeyAuthenticationToken.authenticated(client.name(), client.authorities()))
                .orElseThrow(() -> new BadCredentialsException("Invalid API key"));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            // Every Java runtime has SHA-256
            throw new IllegalStateException(e);
        }
    }

    private record RegisteredClient(String name, byte[] keyHash, List<GrantedAuthority> authorities) {
    }
}

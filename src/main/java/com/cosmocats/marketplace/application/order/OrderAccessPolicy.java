package com.cosmocats.marketplace.application.order;

import com.cosmocats.marketplace.infrastructure.persistence.CosmoCatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Used from @PreAuthorize and @PostAuthorize of OrderService together with the COSMO_CAT role:
 * a cosmo cat works only with its own orders.
 * The user from the token and the cosmo cat in the database are matched by email, which is unique
 * for cosmo cats. This relies on the identity provider putting only verified emails into tokens.
 */
@Component
@RequiredArgsConstructor
public class OrderAccessPolicy {

    private static final String EMAIL_CLAIM = "email";

    private final CosmoCatRepository cosmoCatRepository;

    public boolean isCustomer(Authentication authentication, UUID customerId) {
        // API key clients and tokens without an email are not cosmo cats
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return false;
        }
        String email = jwtAuthentication.getToken().getClaimAsString(EMAIL_CLAIM);
        if (email == null || email.isBlank()) {
            return false;
        }
        return cosmoCatRepository.findByReference(customerId)
                .map(cosmoCat -> cosmoCat.getEmail().equalsIgnoreCase(email))
                .orElse(false);
    }
}

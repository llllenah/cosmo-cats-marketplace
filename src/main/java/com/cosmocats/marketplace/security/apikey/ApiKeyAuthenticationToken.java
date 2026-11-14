package com.cosmocats.marketplace.security.apikey;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

/**
 * Before authentication it carries the key from the request, after it the name and roles of the client.
 */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String clientName;
    private String apiKey;

    private ApiKeyAuthenticationToken(String clientName, String apiKey,
                                      Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.clientName = clientName;
        this.apiKey = apiKey;
    }

    public static ApiKeyAuthenticationToken unauthenticated(String apiKey) {
        return new ApiKeyAuthenticationToken(null, apiKey, List.of());
    }

    public static ApiKeyAuthenticationToken authenticated(String clientName,
                                                          Collection<? extends GrantedAuthority> authorities) {
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(clientName, null, authorities);
        token.setAuthenticated(true);
        return token;
    }

    @Override
    public String getPrincipal() {
        return clientName;
    }

    @Override
    public String getCredentials() {
        return apiKey;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        apiKey = null;
    }
}

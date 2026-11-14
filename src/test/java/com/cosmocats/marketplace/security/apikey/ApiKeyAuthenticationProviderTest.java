package com.cosmocats.marketplace.security.apikey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ApiKeyAuthenticationProvider")
class ApiKeyAuthenticationProviderTest {

    private static final String COMMITTEE_KEY = "committee-test-key";
    private static final String COMMITTEE_KEY_SHA256 = "9c3ab4daf77d5eb2aa8db948f6fc5ae61db834d32b0029c9241728a7c6caf953";
    // SHA-256 of "auditor-test-key", written in upper case like some tools print it
    private static final String AUDITOR_KEY_SHA256 = "56C35EFF6508287BB08DC4AA3A16C1B07927FF4D79C0F5BCEBAB107070796A25";

    private final ApiKeyAuthenticationProvider provider = new ApiKeyAuthenticationProvider(new ApiKeyProperties(clients()));

    @Test
    void authenticatesClientByKeyAndGivesItsRolesWithPrefix() {
        Authentication result = provider.authenticate(ApiKeyAuthenticationToken.unauthenticated(COMMITTEE_KEY));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("intergalactic-committee");
        assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_AUDITOR", "ROLE_COMMITTEE");
        assertThat(result.getCredentials()).isNull();
    }

    @Test
    void findsRightClientAmongSeveral() {
        Authentication result = provider.authenticate(ApiKeyAuthenticationToken.unauthenticated("auditor-test-key"));

        assertThat(result.getName()).isEqualTo("auditor");
        assertThat(result.getAuthorities()).isEmpty();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @NullSource
    @ValueSource(strings = {"", " ", "unknown-key", "COMMITTEE-TEST-KEY", COMMITTEE_KEY_SHA256})
    void rejectsKeyThatNoClientHas(String apiKey) {
        assertThatThrownBy(() -> provider.authenticate(ApiKeyAuthenticationToken.unauthenticated(apiKey)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid API key");
    }

    @Test
    void rejectsTooLongKey() {
        assertThatThrownBy(() -> provider.authenticate(ApiKeyAuthenticationToken.unauthenticated("k".repeat(257))))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void clientWithoutConfiguredKeyCannotLogInWithAnyKey() {
        // SHA-256 of an empty string, in case somebody tries the "empty" key of that client
        assertThatThrownBy(() -> provider.authenticate(ApiKeyAuthenticationToken.unauthenticated(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void supportsOnlyApiKeyTokens() {
        assertThat(provider.supports(ApiKeyAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
    }

    @Test
    void unauthenticatedTokenForgetsKeyWhenCredentialsAreErased() {
        ApiKeyAuthenticationToken token = ApiKeyAuthenticationToken.unauthenticated(COMMITTEE_KEY);
        assertThat(token.isAuthenticated()).isFalse();
        assertThat(token.getPrincipal()).isNull();
        assertThat(token.getCredentials()).isEqualTo(COMMITTEE_KEY);

        token.eraseCredentials();

        assertThat(token.getCredentials()).isNull();
    }

    private static Map<String, ApiKeyProperties.Client> clients() {
        Map<String, ApiKeyProperties.Client> clients = new LinkedHashMap<>();
        clients.put("intergalactic-committee",
                new ApiKeyProperties.Client(COMMITTEE_KEY_SHA256, Set.of("COMMITTEE", "AUDITOR")));
        clients.put("auditor", new ApiKeyProperties.Client(AUDITOR_KEY_SHA256, null));
        clients.put("client-without-key", new ApiKeyProperties.Client("", Set.of("ADMIN")));
        clients.put("client-with-missing-key", new ApiKeyProperties.Client(null, Set.of("ADMIN")));
        return clients;
    }
}

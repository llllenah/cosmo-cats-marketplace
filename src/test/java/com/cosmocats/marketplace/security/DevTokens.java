package com.cosmocats.marketplace.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Plays the identity provider for local runs and tests: signs access tokens with the development
 * key from scripts/security. Its public part is the JWKS file that WireMock serves, so the
 * application accepts these tokens the same way it would accept tokens of a real provider.
 * Used by the tests and by {@code ./gradlew -q devToken -Proles=ADMIN -Pemail=luna@moon.space}.
 */
public final class DevTokens {

    public static final String ISSUER = "http://localhost:8090";
    public static final String AUDIENCE = "cosmo-cats-marketplace";
    public static final String KEY_ID = "cosmo-cats-dev-key";

    private static final Path DEV_PRIVATE_KEY = Path.of("scripts/security/dev-private-key.pem");
    private static final Duration LIFETIME = Duration.ofHours(1);

    private DevTokens() {
    }

    public static void main(String[] args) {
        System.out.println(token(List.of(args[0].split(",")), args[1]));
    }

    public static String token(List<String> roles, String email) {
        return sign(claims(roles, email).build(), devPrivateKey());
    }

    /**
     * Claims of a valid token. Tests change one of them to get a token that must be rejected.
     */
    public static JWTClaimsSet.Builder claims(List<String> roles, String email) {
        Instant now = Instant.now();
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(AUDIENCE)
                .subject(email)
                .claim("email", email)
                .claim("roles", roles)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(LIFETIME)))
                .jwtID(UUID.randomUUID().toString());
    }

    public static String sign(JWTClaimsSet claims, PrivateKey key) {
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(), claims);
        try {
            jwt.sign(new RSASSASigner(key));
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot sign the token", e);
        }
        return jwt.serialize();
    }

    public static PrivateKey devPrivateKey() {
        try {
            String pem = Files.readString(DEV_PRIVATE_KEY)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch (IOException e) {
            throw new UncheckedIOException("Run from the project root, the key is read from " + DEV_PRIVATE_KEY, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot read " + DEV_PRIVATE_KEY, e);
        }
    }
}

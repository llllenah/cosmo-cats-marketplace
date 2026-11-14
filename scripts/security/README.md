# Local identity provider stub

`dev-private-key.pem` signs access tokens for local runs and tests. Its public part is
`scripts/wiremock/__files/jwks.json`, which WireMock serves as `/.well-known/jwks.json`.

The key is for development only: it is public in this repository, so anybody can sign a token with it.
That is why the application trusts it only with the profile `local` (`application-local.yml`) and in tests
(`src/test/resources/config/application.yml`). Without a profile it has no identity provider and does not start.
Other environments set `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWKSETURI` and
`SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUERURI` to a real identity provider and never see this key.

Get a token: `./gradlew -q devToken -Proles=ADMIN`

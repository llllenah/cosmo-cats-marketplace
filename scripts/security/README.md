# Local identity provider stub

`dev-private-key.pem` signs access tokens for local runs and tests. Its public part is
`scripts/wiremock/__files/jwks.json`, which WireMock serves as `/.well-known/jwks.json`.

The key is for development only: it is public in this repository, so anybody can sign a token with it.
Other environments set `JWK_SET_URI` and `JWT_ISSUER` to a real identity provider and never see this key.

Get a token: `./gradlew -q devToken -Proles=ADMIN`

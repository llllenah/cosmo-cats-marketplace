package com.cosmocats.marketplace.security;

import com.cosmocats.marketplace.web.account.AccountController;
import com.cosmocats.marketplace.web.account.AccountMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pages outside /api: the second filter chain, OAuth 2.0 login with GitHub.
 * GitHub itself is not called: the tests check where the application sends the browser,
 * and oauth2Login() puts a user into the session as if GitHub had already confirmed it.
 */
@WebMvcTest(AccountController.class)
@Import({AccountMapper.class, SecurityConfig.class})
@TestPropertySource(properties = "spring.security.oauth2.client.registration.github.client-id=test-client-id")
@DisplayName("Login with GitHub")
class GitHubLoginTest {

    private static final String GITHUB_LOGIN_URL = "http://localhost/oauth2/authorization/github";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void anonymousVisitorIsSentToGitHubLogin() throws Exception {
        mockMvc.perform(get("/account"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(GITHUB_LOGIN_URL));
    }

    @Test
    void swaggerUiIsAlsoBehindLogin() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(GITHUB_LOGIN_URL));
    }

    @Test
    void loginRedirectsToGitHubWithClientIdAndCallbackOfThisApplication() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/github"))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, allOf(
                        startsWith("https://github.com/login/oauth/authorize?"),
                        containsString("response_type=code"),
                        containsString("client_id=test-client-id"),
                        containsString("scope=read:user"),
                        containsString("state="),
                        containsString("redirect_uri=http://localhost/login/oauth2/code/github"))));
    }

    @Test
    void loggedInUserSeesGitHubAccount() throws Exception {
        mockMvc.perform(get("/account").with(oauth2Login().attributes(attributes -> {
                    attributes.put("login", "luna-the-cat");
                    attributes.put("name", "Luna");
                    attributes.put("avatar_url", "https://avatars.githubusercontent.com/u/1?v=4");
                })))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("luna-the-cat"))
                .andExpect(jsonPath("$.name").value("Luna"))
                .andExpect(jsonPath("$.avatarUrl").value("https://avatars.githubusercontent.com/u/1?v=4"));
    }

    @Test
    void gitHubUserWithoutDisplayNameHasNullName() throws Exception {
        mockMvc.perform(get("/account").with(oauth2Login().attributes(attributes -> attributes.put("login", "orion"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("orion"))
                .andExpect(jsonPath("$.name").doesNotExist());
    }

    @Test
    void bearerTokenDoesNotOpenPages() throws Exception {
        // Tokens belong to the /api chain. Here the header is ignored and nobody tries to decode it.
        mockMvc.perform(get("/account").header(HttpHeaders.AUTHORIZATION, "Bearer some-token"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl(GITHUB_LOGIN_URL));
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void sessionRequestsAreProtectedFromCsrf() throws Exception {
        // Unlike /api, pages use a session cookie, so a form from another site must not be able to log the user out
        mockMvc.perform(post("/logout").with(oauth2Login()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/logout").with(oauth2Login()).with(csrf()))
                .andExpect(status().isFound());
    }
}

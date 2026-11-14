package com.cosmocats.marketplace.web.account;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    // Attribute names of the GitHub user API (https://api.github.com/user); "name" may be empty
    public AccountResponseDto toDto(OAuth2User gitHubUser) {
        return new AccountResponseDto(
                gitHubUser.getAttribute("login"),
                gitHubUser.getAttribute("name"),
                gitHubUser.getAttribute("avatar_url")
        );
    }
}

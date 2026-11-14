package com.cosmocats.marketplace.web.account;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Page for people, outside /api: shows who is logged in with GitHub.
 * With the no-auth profile nobody logs in, so the page does not exist.
 */
@RestController
@RequestMapping("/account")
@Profile("!no-auth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountMapper accountMapper;

    @GetMapping
    public AccountResponseDto getAccount(@AuthenticationPrincipal OAuth2User gitHubUser) {
        return accountMapper.toDto(gitHubUser);
    }
}

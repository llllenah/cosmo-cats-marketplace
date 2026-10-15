package com.cosmocats.marketplace.web.cat;

import com.cosmocats.marketplace.application.cat.CosmoCatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cosmo-cats")
@RequiredArgsConstructor
public class CosmoCatController {

    private final CosmoCatService cosmoCatService;
    private final CosmoCatMapper cosmoCatMapper;

    @GetMapping
    public List<CosmoCatResponseDto> getCosmoCats() {
        return cosmoCatService.getCosmoCats().stream().map(cosmoCatMapper::toDto).toList();
    }
}

package com.cosmocats.marketplace.web.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CosmicWordValidator")
class CosmicWordValidatorTest {

    private final CosmicWordValidator validator = new CosmicWordValidator();

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
            "Anti-gravity star yarn ball",
            "Galaxy cosmic milk",
            "Comet tail scratching post",
            "Space helmet",
            "Planet shaped bed",
            "Moon dust catnip",
            "Nebula nap capsule",
            "Orbit tracker",
            "Asteroid chew toy",
            "STAR in upper case",
            "Star-shaped bowl"
    })
    void acceptsNameWithCosmicWord(String name) {
        assertThat(validator.isValid(name, null)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
            "Shooting stars wand",
            "Two galaxies blanket",
            "Comets mobile",
            "Planets night light",
            "Moons of Jupiter feeder",
            "Orbits puzzle",
            "Asteroids ball pit",
            "Nebulae poster"
    })
    void acceptsPluralFormsOfCosmicWords(String name) {
        assertThat(validator.isValid(name, null)).isTrue();
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = {
            "Yarn ball",
            "Starch snack",
            "Spacer for doors",
            "Moonlighting kit",
            ""
    })
    void rejectsNameWithoutCosmicWord(String name) {
        assertThat(validator.isValid(name, null)).isFalse();
    }

    @Test
    void leavesNullCheckToNotBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}

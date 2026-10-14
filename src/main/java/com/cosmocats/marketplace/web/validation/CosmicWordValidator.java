package com.cosmocats.marketplace.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CosmicWordValidator implements ConstraintValidator<CosmicWordCheck, String> {

    // Whole words only: "star" and "stars" are accepted, "starch" is not.
    private static final Pattern COSMIC_WORD = Pattern.compile(
            "\\b(stars?|galax(y|ies)|comets?|cosmic|space|planets?|moons?|nebula[es]?|orbits?|asteroids?)\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return COSMIC_WORD.matcher(value).find();
    }
}

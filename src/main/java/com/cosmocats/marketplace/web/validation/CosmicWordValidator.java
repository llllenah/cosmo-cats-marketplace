package com.cosmocats.marketplace.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class CosmicWordValidator implements ConstraintValidator<CosmicWordCheck, String> {

    // Whole words only: "star" is accepted, "starch" is not.
    private static final Pattern COSMIC_WORD = Pattern.compile(
            "\\b(star|galaxy|comet|cosmic|space|planet|moon|nebula|orbit|asteroid)\\b",
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

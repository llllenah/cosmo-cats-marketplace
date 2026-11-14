package com.cosmocats.marketplace.web.exception;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * One place that builds RFC 9457 problem details, so the errors written by the security filters
 * look the same as the errors of the controllers.
 */
@UtilityClass
public class ProblemFactory {

    private static final String ERROR_TYPE_BASE_URL = "https://cosmo-cats.market/errors/";

    public static ProblemDetail problem(HttpStatus status, String errorType, String title,
                                        String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(ERROR_TYPE_BASE_URL + errorType));
        problem.setTitle(title);
        problem.setInstance(URI.create(instance));
        return problem;
    }
}

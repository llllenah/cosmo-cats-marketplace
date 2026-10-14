package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.AbstractIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Keeps the OpenAPI contract and the controllers in sync: every endpoint of the application
 * must be documented, and every documented operation must exist.
 */
@DisplayName("OpenAPI contract")
class ApiContractIT extends AbstractIT {

    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void documentsExactlyTheEndpointsImplementedByControllers() throws IOException {
        assertThat(implementedOperations()).isEqualTo(documentedOperations());
    }

    private Set<String> implementedOperations() {
        Set<String> operations = new TreeSet<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handlerMethod) -> {
            if (handlerMethod.getBeanType().getPackageName().startsWith("com.cosmocats.marketplace")) {
                mapping.getMethodsCondition().getMethods().forEach(method ->
                        mapping.getPatternValues().forEach(path -> operations.add(method.name() + " " + path)));
            }
        });
        return operations;
    }

    @SuppressWarnings("unchecked")
    private Set<String> documentedOperations() throws IOException {
        Map<String, Object> contract;
        try (InputStream input = new ClassPathResource("api-specs/cosmo-cats-product-api.yml").getInputStream()) {
            contract = new Yaml().load(input);
        }
        Set<String> operations = new TreeSet<>();
        ((Map<String, Map<String, Object>>) contract.get("paths")).forEach((path, pathItem) ->
                pathItem.keySet().stream()
                        .filter(HTTP_METHODS::contains)
                        .forEach(method -> operations.add(method.toUpperCase() + " " + path)));
        return operations;
    }
}

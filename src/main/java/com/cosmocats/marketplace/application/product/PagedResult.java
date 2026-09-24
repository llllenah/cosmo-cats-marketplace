package com.cosmocats.marketplace.application.product;

import java.util.List;
import java.util.function.Function;

public record PagedResult<T>(List<T> content, int page, int size, long totalElements) {

    public PagedResult {
        content = List.copyOf(content);
    }

    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }

    public <R> PagedResult<R> map(Function<T, R> mapper) {
        return new PagedResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}

package com.cosmocats.marketplace.application.product;

import java.util.List;

/**
 * One page of results. {@code nextCursor} is null when this is the last page.
 */
public record PagedResult<T>(List<T> content, int size, long totalElements, PageCursor nextCursor) {

    public PagedResult {
        content = List.copyOf(content);
    }
}

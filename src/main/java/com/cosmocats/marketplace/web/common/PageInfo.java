package com.cosmocats.marketplace.web.common;

/**
 * Pagination metadata. nextPageToken is null on the last page.
 */
public record PageInfo(
        int size,
        long totalElements,
        String nextPageToken
) {
}

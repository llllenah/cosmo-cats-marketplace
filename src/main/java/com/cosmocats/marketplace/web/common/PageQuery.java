package com.cosmocats.marketplace.web.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Pagination query parameters shared by list endpoints: ?pageSize=20&pageToken=...
 * The first page is requested without pageToken. Each response returns nextPageToken
 * for the following page.
 */
public record PageQuery(

        @Size(max = 512, message = "must be at most 512 characters")
        String pageToken,

        @Min(value = 1, message = "must be greater than or equal to 1")
        @Max(value = PageQuery.MAX_PAGE_SIZE, message = "must be less than or equal to 100")
        Integer pageSize
) {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public PageQuery {
        pageSize = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }
}

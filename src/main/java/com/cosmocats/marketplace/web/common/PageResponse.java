package com.cosmocats.marketplace.web.common;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        PageInfo page
) {
}

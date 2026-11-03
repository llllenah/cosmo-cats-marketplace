package com.cosmocats.marketplace.infrastructure.persistence.projection;

import java.util.UUID;

/**
 * Projection for the popular products report, getters match the aliases in the query.
 */
public interface PopularProductView {

    UUID getProductId();

    String getProductName();

    Long getTotalQuantity();

    Long getOrderCount();
}

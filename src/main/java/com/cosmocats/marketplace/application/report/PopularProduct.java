package com.cosmocats.marketplace.application.report;

import java.util.UUID;

/**
 * One row of the popular products report.
 *
 * @param totalQuantity how many items of the product were ordered in total
 * @param orderCount    in how many orders the product appears
 */
public record PopularProduct(UUID productId, String productName, long totalQuantity, long orderCount) {
}

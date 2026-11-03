package com.cosmocats.marketplace.application.report;

import com.cosmocats.marketplace.domain.order.OrderStatus;
import com.cosmocats.marketplace.infrastructure.persistence.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Reports for the Intergalactic committee.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductReportService {

    // A cancelled order was not really bought, so it does not count
    private static final Set<OrderStatus> COUNTED_STATUSES = EnumSet.complementOf(EnumSet.of(OrderStatus.CANCELLED));

    private final OrderRepository orderRepository;

    /**
     * Products sorted by the total ordered quantity, the most popular first.
     */
    public List<PopularProduct> getPopularProducts(int limit) {
        return orderRepository.findPopularProducts(COUNTED_STATUSES, Limit.of(limit)).stream()
                .map(view -> new PopularProduct(view.getProductId(), view.getProductName(),
                        view.getTotalQuantity(), view.getOrderCount()))
                .toList();
    }
}

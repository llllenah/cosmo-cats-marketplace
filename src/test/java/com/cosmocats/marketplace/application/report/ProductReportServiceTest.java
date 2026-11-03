package com.cosmocats.marketplace.application.report;

import com.cosmocats.marketplace.domain.order.OrderStatus;
import com.cosmocats.marketplace.infrastructure.persistence.OrderRepository;
import com.cosmocats.marketplace.infrastructure.persistence.projection.PopularProductView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductReportService")
class ProductReportServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ProductReportService productReportService;

    @Captor
    private ArgumentCaptor<Collection<OrderStatus>> statusesCaptor;

    @Test
    void mapsProjectionRowsKeepingOrderOfQuery() {
        UUID milkId = UUID.randomUUID();
        UUID capsuleId = UUID.randomUUID();
        when(orderRepository.findPopularProducts(statusesCaptor.capture(), eq(Limit.of(2)))).thenReturn(List.of(
                view(milkId, "Galaxy cosmic milk", 12, 5),
                view(capsuleId, "Nebula nap capsule", 3, 3)));

        assertThat(productReportService.getPopularProducts(2)).containsExactly(
                new PopularProduct(milkId, "Galaxy cosmic milk", 12, 5),
                new PopularProduct(capsuleId, "Nebula nap capsule", 3, 3));
    }

    @Test
    void countsEveryOrderExceptCancelled() {
        productReportService.getPopularProducts(10);

        verify(orderRepository).findPopularProducts(statusesCaptor.capture(), eq(Limit.of(10)));
        assertThat(statusesCaptor.getValue())
                .containsExactlyInAnyOrder(OrderStatus.CREATED, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    }

    private static PopularProductView view(UUID productId, String productName, long totalQuantity, long orderCount) {
        return new PopularProductView() {
            @Override
            public UUID getProductId() {
                return productId;
            }

            @Override
            public String getProductName() {
                return productName;
            }

            @Override
            public Long getTotalQuantity() {
                return totalQuantity;
            }

            @Override
            public Long getOrderCount() {
                return orderCount;
            }
        };
    }
}

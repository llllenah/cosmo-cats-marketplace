package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.domain.order.OrderStatus;
import com.cosmocats.marketplace.infrastructure.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.infrastructure.persistence.projection.PopularProductView;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Orders are found by their natural id (cart id) with {@link #findByNaturalId(Object)}.
 */
public interface OrderRepository extends NaturalIdRepository<OrderEntity, Long, UUID> {

    boolean existsByItemsProductReference(UUID productReference);

    /**
     * Report for the Intergalactic committee: which products are bought most often.
     * Only the four report columns are selected, the order and product entities are not loaded.
     */
    @Query("""
            select p.reference as productId,
                   p.name as productName,
                   sum(i.quantity) as totalQuantity,
                   count(o) as orderCount
            from OrderEntity o
              join o.items i
              join i.product p
            where o.status in :statuses
            group by p.reference, p.name
            order by sum(i.quantity) desc, count(o) desc, p.name
            """)
    List<PopularProductView> findPopularProducts(Collection<OrderStatus> statuses, Limit limit);
}

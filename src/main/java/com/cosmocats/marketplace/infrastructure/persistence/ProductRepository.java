package com.cosmocats.marketplace.infrastructure.persistence;

import com.cosmocats.marketplace.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    @EntityGraph(attributePaths = "category")
    Optional<ProductEntity> findByReference(UUID reference);

    /**
     * Checks if another product of the category already has this name, ignoring case.
     * lower() instead of IgnoreCase in the method name: Spring Data would use upper(),
     * and the index idx_product_name_lower is built on lower(name).
     */
    @Query("""
            select case when count(p) > 0 then true else false end from ProductEntity p
            where p.category.reference = :categoryReference
              and lower(p.name) = lower(:name)
              and p.reference <> :excludedReference
            """)
    boolean existsByNameInCategory(UUID categoryReference, String name, UUID excludedReference);

    /**
     * First page of keyset pagination: products sorted by name (ignoring case) and reference.
     */
    @Query("""
            select p from ProductEntity p join fetch p.category
            order by lower(p.name), p.reference
            """)
    List<ProductEntity> findFirstPage(Limit limit);

    /**
     * Next page of keyset pagination: products that go after the given name and reference
     * in the same order as {@link #findFirstPage(Limit)}.
     */
    @Query("""
            select p from ProductEntity p join fetch p.category
            where lower(p.name) > lower(:name)
               or (lower(p.name) = lower(:name) and p.reference > :reference)
            order by lower(p.name), p.reference
            """)
    List<ProductEntity> findPageAfter(String name, UUID reference, Limit limit);

    @EntityGraph(attributePaths = "category")
    List<ProductEntity> findByCategoryReferenceOrderByNameAscReferenceAsc(UUID categoryReference);
}

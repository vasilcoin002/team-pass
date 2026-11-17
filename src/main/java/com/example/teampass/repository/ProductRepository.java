package com.example.teampass.repository;

import com.example.teampass.model.product.Currency;
import com.example.teampass.model.product.Price;
import com.example.teampass.model.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    
    Optional<Product> findByName(String name);

    List<Product> findAllByMaxSeatsCountLessThanEqual(Integer maxSeatsCount);
    List<Product> findAllByPriceAmountLessThanEqualAndPriceCurrency(BigDecimal amount, Currency currency);

    @Query("""
        SELECT p FROM Product p
        JOIN p.categories c
        WHERE c.id IN :categoryIds
        GROUP BY p
        HAVING COUNT(DISTINCT c.id) = :categoryCount
    """)
    List<Product> findAllByAllCategories(
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("categoryCount") long categoryCount
    );
}

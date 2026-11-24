package com.jovany.bakery.inventory;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockItemRepository extends JpaRepository<StockItem, Long> {

    Optional<StockItem> findBySku(String sku);

    /**
     * Subtracts stock in ONE atomic UPDATE that only succeeds when enough units exist,
     * so two concurrent orders can never both take the last cake.
     *
     * @return 1 if the stock was reserved, 0 if there were not enough units
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            update StockItem s
               set s.quantity = s.quantity - :quantity
             where s.sku = :sku and s.quantity >= :quantity
            """)
    int decreaseStock(@Param("sku") String sku, @Param("quantity") int quantity);
}

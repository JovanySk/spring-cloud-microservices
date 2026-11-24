package com.jovany.bakery.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final StockItemRepository repository;

    public InventoryService(StockItemRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(String sku) {
        return StockResponse.from(findItem(sku));
    }

    /**
     * Takes {@code quantity} units out of the stock.
     *
     * @throws StockNotFoundException     if the SKU is unknown (404)
     * @throws InsufficientStockException if there are not enough units (409)
     */
    @Transactional
    public ReservationResponse reserve(String sku, int quantity) {
        StockItem item = findItem(sku);
        int updatedRows = repository.decreaseStock(sku, quantity);
        if (updatedRows == 0) {
            throw new InsufficientStockException(sku, quantity, item.getQuantity());
        }
        int remaining = findItem(sku).getQuantity();
        return new ReservationResponse(sku, quantity, remaining);
    }

    private StockItem findItem(String sku) {
        return repository.findBySku(sku).orElseThrow(() -> new StockNotFoundException(sku));
    }
}

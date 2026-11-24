package com.jovany.bakery.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<ProductResponse> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(ProductResponse::from).toList();
    }

    public ProductResponse findById(Long id) {
        return repository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException("Product " + id + " not found"));
    }

    public ProductResponse findBySku(String sku) {
        return repository.findBySku(sku)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ProductNotFoundException("Product with SKU " + sku + " not found"));
    }
}

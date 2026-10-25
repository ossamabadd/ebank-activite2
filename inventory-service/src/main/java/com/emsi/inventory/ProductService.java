package com.emsi.inventory;

import static com.emsi.inventory.ProductDtos.*;
import java.util.List;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository products;
    public ProductService(ProductRepository products) { this.products = products; }

    public List<ProductView> list(String search, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        return products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(search, search,
                PageRequest.of(page, size, Sort.by("id"))).map(this::view).getContent();
    }
    public ProductView get(Long id) { return view(required(id)); }
    @Transactional
    public ProductView create(ProductRequest request) {
        return view(products.saveAndFlush(new Product(request.sku(), request.name().trim(),
                request.unitPrice(), request.quantity())));
    }
    @Transactional
    public ProductView update(Long id, ProductRequest request) {
        var product = required(id);
        product.sku = request.sku(); product.name = request.name().trim();
        product.unitPrice = request.unitPrice(); product.quantity = request.quantity();
        return view(products.saveAndFlush(product));
    }
    @Transactional
    public void delete(Long id) { products.delete(required(id)); }
    private Product required(Long id) {
        return products.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }
    private ProductView view(Product p) { return new ProductView(p.id, p.sku, p.name, p.unitPrice, p.quantity); }
}

package com.emsi.inventory;

import static com.emsi.inventory.ProductDtos.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;
    public ProductController(ProductService products) { this.products = products; }
    @GetMapping
    List<ProductView> list(@RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return products.list(search, page, size);
    }
    @GetMapping("/{id}") ProductView get(@PathVariable Long id) { return products.get(id); }
    @PostMapping ResponseEntity<ProductView> create(@Valid @RequestBody ProductRequest request) {
        var result = products.create(request);
        return ResponseEntity.created(URI.create("/api/products/" + result.id())).body(result);
    }
    @PutMapping("/{id}") ProductView update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return products.update(id, request);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) { products.delete(id); }
}

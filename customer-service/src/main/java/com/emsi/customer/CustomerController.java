package com.emsi.customer;

import com.emsi.common.CustomerView;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerService service;
    public CustomerController(CustomerService service) { this.service = service; }

    @GetMapping
    List<CustomerView> list(@RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.search(name, page, size);
    }

    @GetMapping("/{id}")
    CustomerView get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    ResponseEntity<CustomerView> create(@Valid @RequestBody CustomerRequest request) {
        var created = service.create(request);
        return ResponseEntity.created(URI.create("/api/customers/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    CustomerView update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return service.update(id, request);
    }
}

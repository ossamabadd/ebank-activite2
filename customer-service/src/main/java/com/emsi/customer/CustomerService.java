package com.emsi.customer;

import com.emsi.common.CustomerView;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CustomerService {
    private final CustomerRepository customers;
    public CustomerService(CustomerRepository customers) { this.customers = customers; }

    public List<CustomerView> search(String name, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page >= 0 and size between 1 and 100 required");
        }
        return customers.findByNameContainingIgnoreCase(name == null ? "" : name,
                PageRequest.of(page, size, Sort.by("id"))).map(this::view).getContent();
    }

    public CustomerView get(Long id) { return view(required(id)); }

    @Transactional
    public CustomerView create(CustomerRequest request) {
        return view(customers.saveAndFlush(new Customer(request.name().trim(),
                request.email().trim().toLowerCase(Locale.ROOT))));
    }

    @Transactional
    public CustomerView update(Long id, CustomerRequest request) {
        var customer = required(id);
        customer.update(request.name().trim(), request.email().trim().toLowerCase(Locale.ROOT));
        return view(customers.saveAndFlush(customer));
    }

    private Customer required(Long id) {
        return customers.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id));
    }

    private CustomerView view(Customer c) {
        return new CustomerView(c.getId(), c.getName(), c.getEmail(), true);
    }
}

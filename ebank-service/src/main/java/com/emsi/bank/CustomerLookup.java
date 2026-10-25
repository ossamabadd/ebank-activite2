package com.emsi.bank;

import com.emsi.common.CustomerView;
import feign.FeignException;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerLookup {
    private final CustomerClient client;
    private final CircuitBreakerFactory<?, ?> breakers;
    public CustomerLookup(CustomerClient client, CircuitBreakerFactory<?, ?> breakers) {
        this.client = client; this.breakers = breakers;
    }

    public CustomerView get(Long id) {
        return breakers.create("customerLookup").run(() -> client.get(id), failure -> {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof FeignException feign && feign.status() == 404) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found: " + id);
                }
            }
            return CustomerView.unavailable(id);
        });
    }

    public CustomerView requireAvailable(Long id) {
        var customer = get(id);
        if (!customer.available()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Customer service unavailable; account creation cannot be validated");
        }
        return customer;
    }
}

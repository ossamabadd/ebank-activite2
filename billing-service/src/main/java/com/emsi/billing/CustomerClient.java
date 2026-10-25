package com.emsi.billing;

import com.emsi.common.CustomerView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "customer-service", url = "${app.customer-url:}")
public interface CustomerClient {
    @GetMapping("/api/customers/{id}") CustomerView get(@PathVariable("id") Long id);
}

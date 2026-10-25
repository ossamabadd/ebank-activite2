package com.emsi.billing;

import static com.emsi.billing.BillDtos.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "inventory-service", url = "${app.inventory-url:}")
public interface InventoryClient {
    @GetMapping("/api/products/{id}") ProductView get(@PathVariable("id") Long id);
}

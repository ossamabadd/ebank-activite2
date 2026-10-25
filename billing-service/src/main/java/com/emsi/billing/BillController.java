package com.emsi.billing;

import static com.emsi.billing.BillDtos.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bills")
public class BillController {
    private final BillService bills;
    public BillController(BillService bills) { this.bills = bills; }
    @GetMapping List<BillView> list(@RequestParam(required = false) Long customerId,
            @RequestParam(required = false) BillStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return bills.list(customerId, status, page, size);
    }
    @GetMapping("/{id}") BillView get(@PathVariable Long id) { return bills.get(id); }
    @PostMapping ResponseEntity<BillView> create(@RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CreateBill request) {
        var result = bills.create(key, request);
        return ResponseEntity.created(URI.create("/api/bills/" + result.id())).body(result);
    }
    @PatchMapping("/{id}/status") BillView status(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return bills.changeStatus(id, request.status());
    }
}

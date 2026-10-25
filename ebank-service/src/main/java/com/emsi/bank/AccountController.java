package com.emsi.bank;

import static com.emsi.bank.BankDtos.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service = service; }

    @GetMapping
    List<AccountView> list(@RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.list(customerId, page, size);
    }
    @GetMapping("/{id}")
    AccountView get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping
    ResponseEntity<AccountView> create(@Valid @RequestBody CreateAccount request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/accounts/" + result.id())).body(result);
    }
    @PostMapping("/{id}/credit")
    OperationView credit(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody MoneyRequest request) { return service.changeBalance(id, key, request, true); }
    @PostMapping("/{id}/debit")
    OperationView debit(@PathVariable UUID id, @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody MoneyRequest request) { return service.changeBalance(id, key, request, false); }
    @GetMapping("/{id}/operations")
    List<OperationView> history(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) { return service.history(id, page, size); }
}

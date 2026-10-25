package com.emsi.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public final class BillDtos {
    private BillDtos() {}
    public record LineRequest(@NotNull @Positive Long productId, @Min(1) @Max(1000000) int quantity) {}
    public record CreateBill(@NotNull @Positive Long customerId, @NotNull @FutureOrPresent LocalDate dueDate,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid LineRequest> items) {}
    public record StatusRequest(@NotNull BillStatus status) {}
    public record ItemView(Long id, Long productId, String sku, String productName, int quantity,
            BigDecimal unitPrice, BigDecimal lineTotal) {}
    public record BillView(Long id, String number, Instant issuedAt, LocalDate dueDate, BillStatus status,
            Long customerId, String customerName, String customerEmail, String currency,
            BigDecimal total, List<ItemView> items) {}
    public record ProductView(Long id, String sku, String name, BigDecimal unitPrice, int quantity) {}
}

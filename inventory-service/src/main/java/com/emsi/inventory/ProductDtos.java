package com.emsi.inventory;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public final class ProductDtos {
    private ProductDtos() {}
    public record ProductRequest(
            @NotBlank @Pattern(regexp = "[A-Z0-9][A-Z0-9_-]{1,39}") String sku,
            @NotBlank @Size(max = 160) String name,
            @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
            @Min(0) @Max(1000000) int quantity) {}
    public record ProductView(Long id, String sku, String name, BigDecimal unitPrice, int quantity) {}
}

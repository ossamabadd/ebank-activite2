package com.emsi.bank;

import com.emsi.common.CustomerView;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BankDtos {
    private BankDtos() {}
    public record CreateAccount(@NotNull @Positive Long customerId, @NotNull AccountType type,
            @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
            @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal balance) {}
    public record AccountView(UUID id, BigDecimal balance, String currency, AccountType type,
            Instant createdAt, Long customerId, CustomerView customer) {}
    public record MoneyRequest(@NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {}
    public record OperationView(Long id, UUID accountId, String type, BigDecimal amount,
            BigDecimal balanceAfter, Instant createdAt) {}
}

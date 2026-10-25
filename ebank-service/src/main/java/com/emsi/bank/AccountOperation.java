package com.emsi.bank;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_operations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "request_key"}))
public class AccountOperation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(name = "account_id", nullable = false)
    UUID accountId;
    @Column(name = "request_key", nullable = false, length = 100)
    String requestKey;
    @Column(nullable = false)
    String type;
    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal amount;
    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal balanceAfter;
    @Column(nullable = false)
    Instant createdAt;

    protected AccountOperation() {}
    AccountOperation(UUID accountId, String requestKey, String type, BigDecimal amount, BigDecimal balanceAfter) {
        this.accountId = accountId; this.requestKey = requestKey; this.type = type;
        this.amount = amount; this.balanceAfter = balanceAfter; this.createdAt = Instant.now();
    }
}

package com.emsi.bank;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {
    @Id
    UUID id;
    @Column(nullable = false)
    Long customerId;
    @Column(nullable = false, precision = 19, scale = 2)
    BigDecimal balance;
    @Column(nullable = false, length = 3)
    String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    AccountType type;
    @Column(nullable = false)
    Instant createdAt;
    @Version
    Long version;

    protected BankAccount() {}
    public BankAccount(UUID id, Long customerId, BigDecimal balance, String currency, AccountType type) {
        this.id = id; this.customerId = customerId; this.balance = balance;
        this.currency = currency; this.type = type; this.createdAt = Instant.now();
    }
}

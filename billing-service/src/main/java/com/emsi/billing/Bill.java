package com.emsi.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "bills", uniqueConstraints = @UniqueConstraint(columnNames = "request_key"))
public class Bill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "request_key", nullable = false, length = 100) String requestKey;
    @Column(nullable = false, length = 64) String requestFingerprint;
    @Column(nullable = false) Long customerId;
    @Column(nullable = false, length = 120) String customerName;
    @Column(nullable = false, length = 200) String customerEmail;
    @Column(nullable = false) Instant issuedAt;
    @Column(nullable = false) LocalDate dueDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false) BillStatus status = BillStatus.UNPAID;
    @Column(nullable = false, precision = 24, scale = 2) BigDecimal total;
    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    List<BillItem> items = new ArrayList<>();
    @Version Long version;

    protected Bill() {}
    Bill(String requestKey, String fingerprint, Long customerId, String customerName,
            String customerEmail, LocalDate dueDate) {
        this.requestKey = requestKey; this.requestFingerprint = fingerprint; this.customerId = customerId;
        this.customerName = customerName; this.customerEmail = customerEmail;
        this.issuedAt = Instant.now(); this.dueDate = dueDate; this.total = BigDecimal.ZERO;
    }
    void add(BillItem item) { item.bill = this; items.add(item); total = total.add(item.lineTotal); }
}

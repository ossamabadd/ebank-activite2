package com.emsi.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "bill_items")
public class BillItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "bill_id") Bill bill;
    @Column(nullable = false) Long productId;
    @Column(nullable = false, length = 40) String sku;
    @Column(nullable = false, length = 160) String productName;
    @Column(nullable = false) int quantity;
    @Column(nullable = false, precision = 14, scale = 2) BigDecimal unitPrice;
    @Column(nullable = false, precision = 24, scale = 2) BigDecimal lineTotal;

    protected BillItem() {}
    BillItem(Long productId, String sku, String productName, int quantity, BigDecimal unitPrice) {
        this.productId = productId; this.sku = sku; this.productName = productName;
        this.quantity = quantity; this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}

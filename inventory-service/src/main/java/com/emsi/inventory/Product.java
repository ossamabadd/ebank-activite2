package com.emsi.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "sku"))
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, length = 40) String sku;
    @Column(nullable = false, length = 160) String name;
    @Column(nullable = false, precision = 14, scale = 2) BigDecimal unitPrice;
    @Column(nullable = false) int quantity;
    @Version Long version;

    protected Product() {}
    public Product(String sku, String name, BigDecimal unitPrice, int quantity) {
        this.sku = sku; this.name = name; this.unitPrice = unitPrice; this.quantity = quantity;
    }
}

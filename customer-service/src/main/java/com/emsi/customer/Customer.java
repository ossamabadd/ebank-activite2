package com.emsi.customer;

import jakarta.persistence.*;

@Entity
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Customer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 200)
    private String email;

    protected Customer() {}
    public Customer(String name, String email) { this.name = name; this.email = email; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public void update(String name, String email) { this.name = name; this.email = email; }
}

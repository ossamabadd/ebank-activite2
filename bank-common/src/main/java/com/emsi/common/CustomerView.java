package com.emsi.common;

public record CustomerView(Long id, String name, String email, boolean available) {
    public static CustomerView unavailable(Long id) {
        return new CustomerView(id, "Customer service unavailable", null, false);
    }
}

package com.emsi.customer;

import jakarta.validation.constraints.*;

public record CustomerRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 200) String email) {}

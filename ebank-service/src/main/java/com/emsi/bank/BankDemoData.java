package com.emsi.bank;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class BankDemoData {
    @Bean
    CommandLineRunner seedAccounts(BankAccountRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(
                        new BankAccount(UUID.fromString("11111111-1111-1111-1111-111111111111"), 1L,
                                new BigDecimal("12500.00"), "MAD", AccountType.CURRENT_ACCOUNT),
                        new BankAccount(UUID.fromString("22222222-2222-2222-2222-222222222222"), 2L,
                                new BigDecimal("32500.00"), "MAD", AccountType.SAVING_ACCOUNT),
                        new BankAccount(UUID.fromString("33333333-3333-3333-3333-333333333333"), 1L,
                                new BigDecimal("900.00"), "EUR", AccountType.SAVING_ACCOUNT)));
            }
        };
    }
}

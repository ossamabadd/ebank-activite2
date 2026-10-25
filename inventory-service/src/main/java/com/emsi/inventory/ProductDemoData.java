package com.emsi.inventory;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class ProductDemoData {
    @Bean CommandLineRunner seedProducts(ProductRepository products) {
        return args -> {
            if (products.count() == 0) products.saveAll(List.of(
                    new Product("LAP-001", "MacBook Air M3", new BigDecimal("12990.00"), 12),
                    new Product("MON-002", "Ecran Dell UltraSharp 27", new BigDecimal("3490.00"), 24),
                    new Product("KEY-003", "Clavier Logitech MX Keys", new BigDecimal("1190.00"), 38),
                    new Product("MOU-004", "Souris Logitech MX Master", new BigDecimal("890.00"), 45),
                    new Product("HUB-005", "Hub USB-C 7 ports", new BigDecimal("490.00"), 64)));
        };
    }
}

package com.emsi.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class BillDemoData {
    @Bean CommandLineRunner seedBills(BillRepository repository) {
        return args -> {
            if (repository.count() != 0) return;
            var first = new Bill("demo-1", "demo", 1L, "Hassan", "hassan@example.com", LocalDate.now().plusDays(15));
            first.add(new BillItem(1L, "LAP-001", "MacBook Air M3", 2, new BigDecimal("12990.00")));
            first.add(new BillItem(5L, "HUB-005", "Hub USB-C 7 ports", 2, new BigDecimal("490.00")));
            var second = new Bill("demo-2", "demo", 2L, "Imane", "imane@example.com", LocalDate.now().plusDays(20));
            second.add(new BillItem(2L, "MON-002", "Ecran Dell UltraSharp 27", 3, new BigDecimal("3490.00")));
            second.status = BillStatus.PAID;
            var third = new Bill("demo-3", "demo", 3L, "Mohamed", "mohamed@example.com", LocalDate.now().plusDays(7));
            third.add(new BillItem(3L, "KEY-003", "Clavier Logitech MX Keys", 4, new BigDecimal("1190.00")));
            third.add(new BillItem(4L, "MOU-004", "Souris Logitech MX Master", 4, new BigDecimal("890.00")));
            repository.saveAll(List.of(first, second, third));
        };
    }
}

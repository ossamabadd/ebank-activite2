package com.emsi.customer;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class CustomerDemoData {
    @Bean
    CommandLineRunner seedCustomers(CustomerRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                repository.saveAll(List.of(new Customer("Hassan", "hassan@example.com"),
                        new Customer("Imane", "imane@example.com"),
                        new Customer("Mohamed", "mohamed@example.com")));
            }
        };
    }
}

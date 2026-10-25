package com.emsi.customer;

import com.emsi.common.CustomerView;
import java.util.List;
import org.springframework.ai.tool.annotation.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

@Component
public class CustomerTools {
    private final CustomerService customers;
    public CustomerTools(CustomerService customers) { this.customers = customers; }

    @Tool(description = "Find customers by name (empty string lists customers). Returns at most 100 customers per page.")
    public List<CustomerView> searchCustomers(
            @ToolParam(description = "Full or partial customer name, or empty string") String name,
            @ToolParam(description = "Zero-based page number") int page) {
        return customers.search(name, page, 100);
    }

    @Tool(description = "Get a customer by their numeric customer ID.")
    public CustomerView getCustomer(@ToolParam(description = "Customer ID") Long customerId) {
        return customers.get(customerId);
    }

    @Configuration
    static class ToolsConfiguration {
        @Bean
        ToolCallbackProvider customerToolCallbacks(CustomerTools tools) {
            return MethodToolCallbackProvider.builder().toolObjects(tools).build();
        }
    }
}

package com.emsi.bank;

import static com.emsi.bank.BankDtos.*;
import java.util.*;
import org.springframework.ai.tool.annotation.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Component;

@Component
public class BankTools {
    private final AccountService accounts;
    public BankTools(AccountService accounts) { this.accounts = accounts; }

    @Tool(description = "List bank accounts, optionally filtered by numeric customer ID. Use customerId 0 to list all. At most 100 per page.")
    public List<AccountView> listAccounts(@ToolParam(description = "Customer ID, or 0 for all customers") long customerId,
            @ToolParam(description = "Zero-based page number") int page) {
        return accounts.list(customerId == 0 ? null : customerId, page, 100);
    }

    @Tool(description = "Get one bank account, its balance, currency and customer, by account UUID.")
    public AccountView getAccount(@ToolParam(description = "Bank account UUID") UUID accountId) {
        return accounts.get(accountId);
    }

    @Tool(description = "Read credit and debit history for an account, at most 100 entries per page.")
    public List<OperationView> accountHistory(@ToolParam(description = "Bank account UUID") UUID accountId,
            @ToolParam(description = "Zero-based page number") int page) {
        return accounts.history(accountId, page, 100);
    }

    @Configuration
    static class ToolsConfiguration {
        @Bean
        ToolCallbackProvider bankToolCallbacks(BankTools tools) {
            return MethodToolCallbackProvider.builder().toolObjects(tools).build();
        }
    }
}

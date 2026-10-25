package com.emsi.bank;

import static com.emsi.bank.BankDtos.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AccountService {
    private static final BigDecimal MAX_BALANCE = new BigDecimal("99999999999999999.99");
    private final BankAccountRepository accounts;
    private final OperationRepository operations;
    private final CustomerLookup customers;
    public AccountService(BankAccountRepository accounts, OperationRepository operations, CustomerLookup customers) {
        this.accounts = accounts; this.operations = operations; this.customers = customers;
    }

    public List<AccountView> list(Long customerId, int page, int size) {
        var pageable = page(page, size, "createdAt");
        var result = customerId == null ? accounts.findAll(pageable)
                : accounts.findByCustomerId(customerId, pageable);
        return result.map(this::view).getContent();
    }

    public AccountView get(UUID id) { return view(required(id)); }

    @Transactional
    public AccountView create(CreateAccount request) {
        var customer = customers.requireAvailable(request.customerId());
        try { Currency.getInstance(request.currency()); }
        catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown ISO 4217 currency");
        }
        var account = accounts.save(new BankAccount(UUID.randomUUID(), request.customerId(),
                request.balance(), request.currency(), request.type()));
        return new AccountView(account.id, account.balance, account.currency, account.type,
                account.createdAt, account.customerId, customer);
    }

    @Transactional
    public OperationView changeBalance(UUID id, String key, MoneyRequest request, boolean credit) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid Idempotency-Key header required (1-100 characters)");
        }
        var account = accounts.lockById(id).orElseThrow(() -> missing(id));
        String type = credit ? "CREDIT" : "DEBIT";
        var previous = operations.findByAccountIdAndRequestKey(id, key);
        if (previous.isPresent()) {
            var op = previous.get();
            if (!op.type.equals(type) || op.amount.compareTo(request.amount()) != 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used for a different operation");
            }
            return operationView(op);
        }
        BigDecimal balance = credit ? account.balance.add(request.amount()) : account.balance.subtract(request.amount());
        if (balance.signum() < 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds");
        if (balance.compareTo(MAX_BALANCE) > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Maximum balance exceeded");
        account.balance = balance;
        var operation = operations.save(new AccountOperation(id, key, type, request.amount(), balance));
        return operationView(operation);
    }

    public List<OperationView> history(UUID id, int page, int size) {
        required(id);
        return operations.findByAccountId(id, page(page, size, "id")).map(this::operationView).getContent();
    }

    private Pageable page(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "page >= 0 and size between 1 and 100 required");
        return PageRequest.of(page, size, Sort.by(sort).ascending());
    }

    private BankAccount required(UUID id) { return accounts.findById(id).orElseThrow(() -> missing(id)); }
    private ResponseStatusException missing(UUID id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found: " + id);
    }
    private AccountView view(BankAccount a) {
        return new AccountView(a.id, a.balance, a.currency, a.type, a.createdAt, a.customerId, customers.get(a.customerId));
    }
    private OperationView operationView(AccountOperation op) {
        return new OperationView(op.id, op.accountId, op.type, op.amount, op.balanceAfter, op.createdAt);
    }
}

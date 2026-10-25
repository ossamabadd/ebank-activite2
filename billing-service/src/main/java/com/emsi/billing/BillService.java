package com.emsi.billing;

import static com.emsi.billing.BillDtos.*;
import com.emsi.common.CustomerView;
import feign.FeignException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class BillService {
    private final BillRepository bills;
    private final CustomerClient customers;
    private final InventoryClient inventory;
    public BillService(BillRepository bills, CustomerClient customers, InventoryClient inventory) {
        this.bills = bills; this.customers = customers; this.inventory = inventory;
    }
    public List<BillView> list(Long customerId, BillStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pagination");
        return bills.search(customerId, status, PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("issuedAt"), Sort.Order.desc("id")))).map(this::view).getContent();
    }
    public BillView get(Long id) { return view(required(id)); }

    @Transactional
    public BillView create(String key, CreateBill request) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{1,100}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid Idempotency-Key header required");
        String fingerprint = fingerprint(request);
        var previous = bills.findByRequestKey(key);
        if (previous.isPresent()) {
            if (!previous.get().requestFingerprint.equals(fingerprint))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used for another invoice");
            return view(previous.get());
        }
        var productIds = new HashSet<Long>();
        for (var line : request.items()) {
            if (!productIds.add(line.productId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each product must appear only once");
        }
        CustomerView customer = remote(() -> customers.get(request.customerId()), "Customer");
        if (!customer.available()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Customer unavailable");
        var bill = new Bill(key, fingerprint, customer.id(), customer.name(), customer.email(), request.dueDate());
        for (var line : request.items()) {
            var product = remote(() -> inventory.get(line.productId()), "Product");
            bill.add(new BillItem(product.id(), product.sku(), product.name(), line.quantity(), product.unitPrice()));
        }
        return view(bills.saveAndFlush(bill));
    }

    @Transactional
    public BillView changeStatus(Long id, BillStatus next) {
        var bill = bills.lockById(id).orElseThrow(() -> missing(id));
        if (bill.status != next && (bill.status != BillStatus.UNPAID || next == BillStatus.UNPAID))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Paid or cancelled invoices cannot be changed");
        bill.status = next;
        return view(bill);
    }
    private <T> T remote(Supplier<T> action, String entity) {
        try { return action.get(); }
        catch (FeignException ex) {
            if (ex.status() == 404)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, entity + " not found");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    entity + " service unavailable; no invoice was created");
        }
    }
    private Bill required(Long id) { return bills.findById(id).orElseThrow(() -> missing(id)); }
    private ResponseStatusException missing(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found: " + id);
    }
    private String fingerprint(CreateBill request) {
        StringBuilder value = new StringBuilder().append(request.customerId()).append('|').append(request.dueDate());
        request.items().forEach(line -> value.append('|').append(line.productId()).append(':').append(line.quantity()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private BillView view(Bill b) {
        String number = "FAC-" + b.issuedAt.atZone(ZoneOffset.UTC).getYear() + "-" + String.format("%05d", b.id);
        return new BillView(b.id, number, b.issuedAt, b.dueDate, b.status, b.customerId,
                b.customerName, b.customerEmail, "MAD", b.total,
                b.items.stream().map(i -> new ItemView(i.id, i.productId, i.sku, i.productName,
                        i.quantity, i.unitPrice, i.lineTotal)).toList());
    }
}

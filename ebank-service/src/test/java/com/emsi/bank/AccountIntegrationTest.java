package com.emsi.bank;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.emsi.common.CustomerView;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.ai.mcp.server.enabled=false", "app.seed-data=false"})
@AutoConfigureMockMvc
class AccountIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BankAccountRepository accounts;
    @Autowired OperationRepository operations;
    @Autowired AccountService service;
    @Autowired CircuitBreakerRegistry breakers;
    @MockitoBean CustomerClient customers;
    UUID id;

    @BeforeEach void setup() {
        operations.deleteAll(); accounts.deleteAll();
        breakers.getAllCircuitBreakers().forEach(b -> b.reset());
        when(customers.get(1L)).thenReturn(new CustomerView(1L, "Hassan", "hassan@example.com", true));
        id = accounts.saveAndFlush(new BankAccount(UUID.randomUUID(), 1L,
                new BigDecimal("100.00"), "MAD", AccountType.CURRENT_ACCOUNT)).id;
    }

    @Test void enrichesAccountThroughCustomerClient() throws Exception {
        mvc.perform(get("/api/accounts/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.name").value("Hassan"))
                .andExpect(jsonPath("$.customer.available").value(true));
        verify(customers).get(1L);
    }

    @Test void customerOutagePreservesReadsAndPreventsUnvalidatedCreation() throws Exception {
        when(customers.get(1L)).thenThrow(new IllegalStateException("offline"));
        mvc.perform(get("/api/accounts/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.0))
                .andExpect(jsonPath("$.customer.available").value(false));
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerId":1,"type":"CURRENT_ACCOUNT","currency":"MAD","balance":0}
                """)).andExpect(status().isServiceUnavailable());
    }

    @Test void creditIsIdempotentAndRejectsDifferentReuse() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/accounts/" + id + "/credit").header("Idempotency-Key", "credit-1")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":25.50}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.balanceAfter").value(125.5));
        }
        assertThat(accounts.findById(id).orElseThrow().balance).isEqualByComparingTo("125.50");
        assertThat(operations.count()).isEqualTo(1);
        mvc.perform(post("/api/accounts/" + id + "/credit").header("Idempotency-Key", "credit-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":26}")).andExpect(status().isConflict());
    }

    @Test void missingCustomersReturn404WithoutOpeningTheCircuit() throws Exception {
        var request = feign.Request.create(feign.Request.HttpMethod.GET, "http://customer/api/customers/999",
                Map.of(), new byte[0], java.nio.charset.StandardCharsets.UTF_8, null);
        var response = feign.Response.builder().request(request).status(404).reason("Missing").headers(Map.of()).build();
        when(customers.get(999L)).thenThrow(feign.FeignException.errorStatus("get", response));
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content("""
                    {"customerId":999,"type":"CURRENT_ACCOUNT","currency":"MAD","balance":0}
                    """)).andExpect(status().isNotFound());
        }
        assertThat(breakers.circuitBreaker("customerLookup").getState())
                .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED);
        verify(customers, times(5)).get(999L);
    }

    @Test void repeatedOutageOpensCircuitAndStopsFurtherCalls() {
        when(customers.get(1L)).thenThrow(new IllegalStateException("offline"));
        for (int i = 0; i < 5; i++) assertThat(service.get(id).customer().available()).isFalse();
        assertThat(breakers.circuitBreaker("customerLookup").getState())
                .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
        verify(customers, times(3)).get(1L);
    }

    @Test void debitRejectsOverdraftAndInvalidAmounts() throws Exception {
        mvc.perform(post("/api/accounts/" + id + "/debit").header("Idempotency-Key", "debit-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":101}")).andExpect(status().isConflict());
        mvc.perform(post("/api/accounts/" + id + "/credit").header("Idempotency-Key", "credit-2")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":-1}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/accounts/" + id + "/credit").header("Idempotency-Key", "credit-3")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0.001}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/accounts/" + id + "/credit")
                .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1}")).andExpect(status().isBadRequest());
        assertThat(accounts.findById(id).orElseThrow().balance).isEqualByComparingTo("100.00");
        assertThat(operations.count()).isZero();
    }

    @Test void concurrentCreditsDoNotLoseUpdates() throws Exception {
        var executor = Executors.newFixedThreadPool(4);
        try {
            var jobs = new ArrayList<Callable<Void>>();
            for (int i = 0; i < 8; i++) {
                String key = "concurrent-" + i;
                jobs.add(() -> {
                    service.changeBalance(id, key, new BankDtos.MoneyRequest(new BigDecimal("1.00")), true);
                    return null;
                });
            }
            for (var future : executor.invokeAll(jobs)) future.get(10, TimeUnit.SECONDS);
            assertThat(accounts.findById(id).orElseThrow().balance).isEqualByComparingTo("108.00");
            assertThat(operations.count()).isEqualTo(8);
        } finally { executor.shutdownNow(); }
    }
}

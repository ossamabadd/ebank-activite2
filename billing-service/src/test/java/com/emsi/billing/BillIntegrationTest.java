package com.emsi.billing;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.emsi.common.CustomerView;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false","eureka.client.enabled=false","app.seed-data=false"})
@AutoConfigureMockMvc
class BillIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BillRepository bills;
    @Autowired ObjectMapper json;
    @MockitoBean CustomerClient customers;
    @MockitoBean InventoryClient inventory;

    @BeforeEach void setup() {
        bills.deleteAll();
        when(customers.get(1L)).thenReturn(new CustomerView(1L,"Customer One","one@example.com",true));
        when(inventory.get(1L)).thenReturn(new BillDtos.ProductView(1L,"P-01","Keyboard",new BigDecimal("19.99"),10));
        when(inventory.get(2L)).thenReturn(new BillDtos.ProductView(2L,"P-02","Cable",new BigDecimal("7.50"),10));
    }

    private String body() {
        return "{\"customerId\":1,\"dueDate\":\"" + LocalDate.now().plusDays(10)
                + "\",\"items\":[{\"productId\":1,\"quantity\":3,\"unitPrice\":0.01},{\"productId\":2,\"quantity\":2}]}";
    }
    private Long create(String key) throws Exception {
        var result = mvc.perform(post("/api/bills").header("Idempotency-Key",key)
                .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    @Test void usesFeignPricesAndExactDecimalTotalsAndPreservesSnapshots() throws Exception {
        Long id = create("price-snapshot");
        verify(customers).get(1L); verify(inventory).get(1L); verify(inventory).get(2L);
        clearInvocations(customers,inventory);
        when(inventory.get(1L)).thenReturn(new BillDtos.ProductView(1L,"P-01","Changed",new BigDecimal("999.00"),10));
        when(customers.get(1L)).thenReturn(new CustomerView(1L,"Changed","changed@example.com",true));
        mvc.perform(get("/api/bills/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(74.97))
                .andExpect(jsonPath("$.customerName").value("Customer One"))
                .andExpect(jsonPath("$.items[0].productName").value("Keyboard"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(19.99));
        verifyNoInteractions(customers,inventory);
    }

    @Test void duplicateSubmissionIsIdempotentAndChangedReuseConflicts() throws Exception {
        Long id = create("same-key");
        assertThat(create("same-key")).isEqualTo(id);
        assertThat(bills.count()).isEqualTo(1);
        mvc.perform(post("/api/bills").header("Idempotency-Key","same-key")
                .contentType(MediaType.APPLICATION_JSON).content(body().replace("\"quantity\":3","\"quantity\":4")))
                .andExpect(status().isConflict());
        assertThat(bills.count()).isEqualTo(1);
    }

    @Test void remoteOutageCreatesNoPartialInvoice() throws Exception {
        when(inventory.get(2L)).thenThrow(feignError(503));
        mvc.perform(post("/api/bills").header("Idempotency-Key","outage")
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isServiceUnavailable());
        assertThat(bills.count()).isZero();
    }

    @Test void missingCustomerOrProductReturns404() throws Exception {
        when(customers.get(1L)).thenThrow(feignError(404));
        mvc.perform(post("/api/bills").header("Idempotency-Key","missing-customer")
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isNotFound());
        doReturn(new CustomerView(1L,"Customer","one@example.com",true)).when(customers).get(1L);
        when(inventory.get(1L)).thenThrow(feignError(404));
        mvc.perform(post("/api/bills").header("Idempotency-Key","missing-product")
                .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isNotFound());
        assertThat(bills.count()).isZero();
    }

    @Test void rejectsEmptyLinesInvalidQuantitiesDuplicateProductsAndPastDate() throws Exception {
        String[] invalid = {
                body().replace("\"quantity\":3","\"quantity\":0"),
                body().replace("\"productId\":2","\"productId\":1"),
                "{\"customerId\":1,\"dueDate\":\"" + LocalDate.now().plusDays(1) + "\",\"items\":[]}",
                body().replace(LocalDate.now().plusDays(10).toString(),LocalDate.now().minusDays(1).toString())
        };
        for (int i=0;i<invalid.length;i++)
            mvc.perform(post("/api/bills").header("Idempotency-Key","invalid-" + i)
                    .contentType(MediaType.APPLICATION_JSON).content(invalid[i])).andExpect(status().isBadRequest());
        assertThat(bills.count()).isZero();
    }

    @Test void paidInvoicesAreTerminalAndCanBeFilteredByCustomerAndStatus() throws Exception {
        Long id = create("paid");
        mvc.perform(patch("/api/bills/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(patch("/api/bills/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}")).andExpect(status().isConflict());
        mvc.perform(get("/api/bills").param("customerId","1").param("status","PAID"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/bills").param("status","UNPAID")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test void cancelledInvoicesAreRetainedAndCannotBePaid() throws Exception {
        Long id = create("cancelled");
        mvc.perform(patch("/api/bills/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/bills/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"PAID\"}")).andExpect(status().isConflict());
        mvc.perform(get("/api/bills/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private feign.FeignException feignError(int status) {
        var request = feign.Request.create(feign.Request.HttpMethod.GET,"http://service/test",Map.of(),
                new byte[0],StandardCharsets.UTF_8,null);
        return feign.FeignException.errorStatus("get",feign.Response.builder().request(request)
                .status(status).reason("Test failure").headers(Map.of()).build());
    }
}

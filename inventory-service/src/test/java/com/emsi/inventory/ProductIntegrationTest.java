package com.emsi.inventory;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false","eureka.client.enabled=false","app.seed-data=false"})
@AutoConfigureMockMvc
class ProductIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @BeforeEach void clean() { products.deleteAll(); }

    @Test void createSearchUpdateAndDelete() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"MON-01\",\"name\":\"Monitor\",\"unitPrice\":199.99,\"quantity\":10}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.unitPrice").value(199.99));
        Long id = products.findAll().get(0).id;
        mvc.perform(get("/api/products").param("search","mon")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("MON-01"));
        mvc.perform(put("/api/products/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"MON-01\",\"name\":\"Updated monitor\",\"unitPrice\":210.50,\"quantity\":5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(5));
        mvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
        assertThat(products.count()).isZero();
    }

    @Test void rejectsNegativeStockAndInvalidPriceAndPagination() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"MON-01\",\"name\":\"Monitor\",\"unitPrice\":0,\"quantity\":-1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\"MON-01\",\"name\":\"Monitor\",\"unitPrice\":1.001,\"quantity\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("size","101")).andExpect(status().isBadRequest());
        assertThat(products.count()).isZero();
    }

    @Test void rejectsDuplicateSku() throws Exception {
        String body = "{\"sku\":\"DUP-01\",\"name\":\"Mouse\",\"unitPrice\":20,\"quantity\":1}";
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        assertThat(products.count()).isEqualTo(1);
    }
}

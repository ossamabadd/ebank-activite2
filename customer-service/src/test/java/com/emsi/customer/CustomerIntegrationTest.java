package com.emsi.customer;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false", "app.seed-data=false"})
@AutoConfigureMockMvc
class CustomerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository customers;
    @LocalServerPort int port;

    @BeforeEach void clean() { customers.deleteAll(); }

    @Test void createSearchUpdateAndUniqueEmail() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Imane\",\"email\":\"IMANE@example.com\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value("imane@example.com"));
        long id = customers.findAll().get(0).getId();
        mvc.perform(get("/api/customers").param("name", "ima"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Imane"));
        mvc.perform(put("/api/customers/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Imane Updated\",\"email\":\"imane@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Imane Updated"));
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Duplicate\",\"email\":\"imane@example.com\"}"))
                .andExpect(status().isConflict());
    }

    @Test void validatesInputAndUnknownCustomer() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"email\":\"invalid\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/customers/99999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/customers").param("size", "101")).andExpect(status().isBadRequest());
    }

    @Test void streamableMcpListsAndCallsRealCustomerTools() {
        var created = customers.saveAndFlush(new Customer("MCP Customer", "mcp@example.com"));
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port).endpoint("/mcp").build();
        try (var client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(10)).build()) {
            client.initialize();
            assertThat(client.listTools().tools()).extracting(McpSchema.Tool::name)
                    .contains("getCustomer", "searchCustomers");
            var result = client.callTool(new McpSchema.CallToolRequest("getCustomer", Map.of("customerId", created.getId())));
            assertThat(result.isError()).isNotEqualTo(true);
            assertThat(result.content().toString()).contains("MCP Customer", "mcp@example.com");
        }
    }
}

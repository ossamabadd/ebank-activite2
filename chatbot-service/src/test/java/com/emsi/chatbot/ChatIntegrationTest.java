package com.emsi.chatbot;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.springframework.ai.tool.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.ai.mcp.client.enabled=false", "spring.ai.retry.max-attempts=1",
        "spring.ai.ollama.chat.options.model=test-model"})
@AutoConfigureMockMvc
@Import(ChatIntegrationTest.ToolsConfig.class)
class ChatIntegrationTest {
    static HttpServer model;
    static final ObjectMapper json = new ObjectMapper();
    static final AtomicInteger toolCalls = new AtomicInteger();
    static final AtomicBoolean unavailable = new AtomicBoolean();
    static final List<String> prompts = Collections.synchronizedList(new ArrayList<>());
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        model = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        model.createContext("/api/chat", exchange -> {
            byte[] input = exchange.getRequestBody().readAllBytes();
            String request = new String(input, StandardCharsets.UTF_8);
            prompts.add(request);
            boolean hasToolResult = false;
            for (var message : json.readTree(input).path("messages")) {
                if (message.path("role").asText().equals("tool")) hasToolResult = true;
            }
            Map<String, Object> message = hasToolResult
                    ? Map.of("role", "assistant", "content", "Solde: 42 MAD")
                    : Map.of("role", "assistant", "content", "", "tool_calls", List.of(Map.of(
                            "function", Map.of("name", "lookupBalance", "arguments", Map.of("accountId", "demo")))));
            byte[] output = json.writeValueAsBytes(Map.of("model", "test-model",
                    "created_at", "2026-10-20T09:00:00Z", "message", message, "done", true));
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(unavailable.get() ? 503 : 200, output.length);
            exchange.getResponseBody().write(output);
            exchange.close();
        });
        model.start();
        registry.add("spring.ai.ollama.base-url", () -> "http://127.0.0.1:" + model.getAddress().getPort());
    }

    @AfterAll static void stop() { model.stop(0); }
    @BeforeEach void setup() { toolCalls.set(0); unavailable.set(false); prompts.clear(); }

    @Test void actualSpringAiExecutesToolsAndKeepsConversationsSeparate() throws Exception {
        String id = UUID.randomUUID().toString();
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(
                "{\"message\":\"secret-reference: balance?\",\"conversationId\":\"" + id + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("Solde: 42 MAD"))
                .andExpect(jsonPath("$.conversationId").value(id));
        assertThat(toolCalls.get()).isEqualTo(1);
        assertThat(prompts).anyMatch(p -> p.contains("42 MAD") && p.contains("\"tool\""));
        prompts.clear();
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(
                "{\"message\":\"balance again?\",\"conversationId\":\"" + id + "\"}"))
                .andExpect(status().isOk());
        assertThat(prompts.get(0)).contains("secret-reference");
        prompts.clear();
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"new conversation\"}")).andExpect(status().isOk());
        assertThat(prompts.get(0)).doesNotContain("secret-reference");
        mvc.perform(delete("/api/chat/" + id)).andExpect(status().isNoContent());
        prompts.clear();
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(
                "{\"message\":\"after reset\",\"conversationId\":\"" + id + "\"}"))
                .andExpect(status().isOk());
        assertThat(prompts.get(0)).doesNotContain("secret-reference");
    }

    @Test void unavailableModelReturns503AndBlankInputReturns400() throws Exception {
        unavailable.set(true);
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"balance?\"}")).andExpect(status().isServiceUnavailable());
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\" \"}")).andExpect(status().isBadRequest());
    }

    @TestConfiguration
    static class ToolsConfig {
        @Bean ToolCallbackProvider testTools() {
            return MethodToolCallbackProvider.builder().toolObjects(new BalanceTools()).build();
        }
    }
    static class BalanceTools {
        @Tool(description = "Look up an account balance")
        public String lookupBalance(String accountId) { toolCalls.incrementAndGet(); return "42 MAD"; }
    }
}

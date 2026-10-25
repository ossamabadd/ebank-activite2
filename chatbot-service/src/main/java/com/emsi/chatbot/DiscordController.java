package com.emsi.chatbot;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.*;
import java.net.http.HttpClient;

@RestController
@RequestMapping("/integrations/discord")
@ConditionalOnProperty(name = "app.discord.enabled", havingValue = "true")
public class DiscordController {
    private static final Logger log = LoggerFactory.getLogger(DiscordController.class);
    private final ChatService chat;
    private final ObjectMapper mapper;
    private final DiscordSignatureVerifier verifier;
    private final String applicationId;
    private final RestClient api;
    private final ExecutorService worker = new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(32), new ThreadPoolExecutor.AbortPolicy());

    public DiscordController(ChatService chat, ObjectMapper mapper,
            @Value("${app.discord.public-key}") String publicKey,
            @Value("${app.discord.application-id}") String applicationId) {
        if (!applicationId.matches("[0-9]+")) throw new IllegalArgumentException("DISCORD_APPLICATION_ID is required");
        this.chat = chat; this.mapper = mapper; this.applicationId = applicationId;
        this.verifier = new DiscordSignatureVerifier(publicKey);
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        api = RestClient.builder().requestFactory(factory).baseUrl("https://discord.com/api/v10").build();
    }

    @PostMapping("/interactions")
    Map<String, Object> interaction(@RequestHeader(value = "X-Signature-Ed25519", defaultValue = "") String signature,
            @RequestHeader(value = "X-Signature-Timestamp", defaultValue = "") String timestamp,
            @RequestBody byte[] body) {
        if (!verifier.verify(timestamp, signature, body))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Discord signature");
        JsonNode event = mapper.readTree(body);
        if (event.path("type").asInt() == 1) return Map.of("type", 1);
        if (!applicationId.equals(event.path("application_id").asText())
                || event.path("type").asInt() != 2 || !event.path("data").path("name").asText().equals("bank")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported interaction");
        }
        String question = "";
        for (JsonNode option : event.path("data").path("options")) {
            if (option.path("name").asText().equals("question")) question = option.path("value").asText();
        }
        if (question.isBlank() || question.length() > 4000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Question must contain 1-4000 characters");
        String userId = event.path("member").path("user").path("id").asText();
        if (userId.isBlank()) userId = event.path("user").path("id").asText();
        if (userId.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing user");
        String session = "discord:" + event.path("channel_id").asText() + ":" + userId;
        String token = event.path("token").asText();
        if (!token.matches("[A-Za-z0-9._-]+")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid interaction token");
        final String prompt = question;
        try { worker.submit(() -> respond(session, prompt, token)); }
        catch (RejectedExecutionException ex) {
            return Map.of("type", 4, "data", Map.of("content", "Assistant occupe. Reessayez.", "flags", 64));
        }
        return Map.of("type", 5, "data", Map.of("flags", 64));
    }

    private void respond(String session, String prompt, String token) {
        String answer;
        try { answer = chat.ask(session, prompt); }
        catch (Exception ex) { answer = "Assistant indisponible. Reessayez plus tard."; }
        if (answer.length() > 2000) answer = answer.substring(0, 1999) + "…";
        try {
            api.patch().uri("/webhooks/{id}/{token}/messages/@original", applicationId, token)
                    .body(Map.of("content", answer, "allowed_mentions", Map.of("parse", List.of())))
                    .retrieve().toBodilessEntity();
        } catch (Exception ex) { log.warn("Discord response failed ({})", ex.getClass().getSimpleName()); }
    }

    @PreDestroy
    void stop() { worker.shutdownNow(); }
}

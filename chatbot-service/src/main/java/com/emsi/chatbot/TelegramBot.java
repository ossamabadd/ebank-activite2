package com.emsi.chatbot;

import java.time.Duration;
import java.util.Map;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
@ConditionalOnProperty(name = "app.telegram.enabled", havingValue = "true")
public class TelegramBot {
    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);
    private final RestClient api;
    private final ChatService chat;
    private long offset;

    public TelegramBot(ChatService chat, @Value("${app.telegram.token}") String token) {
        if (token.isBlank()) throw new IllegalArgumentException("TELEGRAM_BOT_TOKEN is required");
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.api = RestClient.builder().requestFactory(factory)
                .baseUrl("https://api.telegram.org/bot" + token).build();
        this.chat = chat;
    }

    @Scheduled(fixedDelayString = "${app.telegram.poll-delay:2000}")
    public void poll() {
        try {
            JsonNode response = api.get().uri("/getUpdates?offset={offset}&timeout=0&limit=10", offset)
                    .retrieve().body(JsonNode.class);
            if (response == null || !response.path("ok").asBoolean()) return;
            for (JsonNode update : response.path("result")) {
                long nextOffset = update.path("update_id").asLong() + 1;
                try { handle(update.path("message")); }
                finally { offset = Math.max(offset, nextOffset); }
            }
        } catch (Exception ex) {
            log.warn("Telegram polling failed ({})", ex.getClass().getSimpleName());
        }
    }

    private void handle(JsonNode message) {
        if (!message.path("chat").path("type").asText().equals("private")
                || !message.has("text") || message.path("from").path("is_bot").asBoolean()) return;
        long chatId = message.path("chat").path("id").asLong();
        String question = message.path("text").asText();
        String session = "telegram:" + chatId;
        String answer;
        if (question.equals("/start")) {
            answer = "Bienvenue sur EBank (demo). Posez une question sur les clients ou comptes. /reset efface la conversation.";
        } else if (question.equals("/reset")) {
            chat.clear(session);
            answer = "Conversation effacee.";
        } else {
            try { answer = chat.ask(session, question); }
            catch (Exception ex) { answer = "Assistant indisponible. Reessayez plus tard."; }
        }
        for (int from = 0; from < answer.length(); from += 4000) {
            api.post().uri("/sendMessage").body(Map.of("chat_id", chatId,
                    "text", answer.substring(from, Math.min(from + 4000, answer.length()))))
                    .retrieve().toBodilessEntity();
        }
    }
}

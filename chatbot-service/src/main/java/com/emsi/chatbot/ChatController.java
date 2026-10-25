package com.emsi.chatbot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {
    private final ChatService chat;
    private final ObjectProvider<ToolCallbackProvider> providers;
    public ChatController(ChatService chat, ObjectProvider<ToolCallbackProvider> providers) {
        this.chat = chat; this.providers = providers;
    }

    public record ChatRequest(@NotBlank @Size(max = 4000) String message, UUID conversationId) {}
    public record ChatResponse(UUID conversationId, String answer) {}
    public record ToolView(String name, String description) {}

    @PostMapping("/api/chat")
    ChatResponse ask(@Valid @RequestBody ChatRequest request) {
        UUID id = request.conversationId() == null ? UUID.randomUUID() : request.conversationId();
        return new ChatResponse(id, chat.ask("rest:" + id, request.message()));
    }

    @DeleteMapping("/api/chat/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void clear(@PathVariable UUID id) { chat.clear("rest:" + id); }

    @GetMapping("/api/tools")
    List<ToolView> tools() {
        return providers.orderedStream().flatMap(p -> Arrays.stream(p.getToolCallbacks()))
                .map(t -> new ToolView(t.getToolDefinition().name(), t.getToolDefinition().description()))
                .toList();
    }
}

package com.emsi.chatbot;

import com.emsi.common.ApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@Import(ApiExceptionHandler.class)
@EnableScheduling
@SpringBootApplication
public class ChatbotApplication {
    public static void main(String[] args) { SpringApplication.run(ChatbotApplication.class, args); }
}

package com.emsi.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
public class GatewayRoutes {
    @Bean
    RouteLocator routes(RouteLocatorBuilder builder, Environment env) {
        boolean fixed = "static".equals(env.getProperty("app.routing-mode", "discovery"));
        return builder.routes()
                .route("customers", r -> r.path("/api/customers", "/api/customers/**")
                        .uri(fixed ? env.getProperty("app.urls.customer", "http://localhost:8081") : "lb://customer-service"))
                .route("inventory", r -> r.path("/api/products", "/api/products/**")
                        .uri(fixed ? env.getProperty("app.urls.inventory", "http://localhost:8084") : "lb://inventory-service"))
                .route("billing", r -> r.path("/api/bills", "/api/bills/**")
                        .uri(fixed ? env.getProperty("app.urls.billing", "http://localhost:8085") : "lb://billing-service"))
                .route("accounts", r -> r.path("/api/accounts", "/api/accounts/**")
                        .uri("lb://ebank-service"))
                .route("chatbot", r -> r.path("/api/chat", "/api/chat/**", "/api/tools",
                                "/integrations/**")
                        .uri("lb://chatbot-service"))
                .build();
    }
}

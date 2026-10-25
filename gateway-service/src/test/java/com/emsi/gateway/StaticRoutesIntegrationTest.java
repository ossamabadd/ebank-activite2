package com.emsi.gateway;

import static org.assertj.core.api.Assertions.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("static")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cloud.config.enabled=false")
class StaticRoutesIntegrationTest {
    @Autowired RouteLocator routes;
    @Test void usesFixedDestinationsWithoutDiscovery() {
        var configured = routes.getRoutes().collectList().block(Duration.ofSeconds(10));
        assertThat(configured).isNotNull().anyMatch(r ->
                r.getId().equals("inventory") && r.getUri().toString().equals("http://localhost:8084"));
        assertThat(configured).anyMatch(r ->
                r.getId().equals("billing") && r.getUri().toString().equals("http://localhost:8085"));
        assertThat(configured).noneMatch(r -> r.getId().contains("DiscoveryClient"));
    }
}

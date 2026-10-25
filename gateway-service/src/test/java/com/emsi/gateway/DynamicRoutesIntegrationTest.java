package com.emsi.gateway;

import static org.assertj.core.api.Assertions.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.cloud.discovery.client.simple.instances.inventory-service[0].uri=http://localhost:9999"})
class DynamicRoutesIntegrationTest {
    @Autowired RouteLocator routes;
    @Test void discoversInventoryWithTheConfiguredRestrictedExpression() {
        var discovered = routes.getRoutes().collectList().block(Duration.ofSeconds(10));
        assertThat(discovered).isNotNull().anyMatch(r ->
                r.getId().contains("inventory-service") && r.getUri().toString().equals("lb://inventory-service"));
    }
}

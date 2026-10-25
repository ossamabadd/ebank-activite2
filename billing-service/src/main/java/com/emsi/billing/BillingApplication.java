package com.emsi.billing;

import com.emsi.common.ApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableFeignClients
@Import(ApiExceptionHandler.class)
public class BillingApplication {
    public static void main(String[] args) { SpringApplication.run(BillingApplication.class, args); }
}

package com.emsi.customer;

import com.emsi.common.ApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(ApiExceptionHandler.class)
@SpringBootApplication
public class CustomerApplication {
    public static void main(String[] args) { SpringApplication.run(CustomerApplication.class, args); }
}

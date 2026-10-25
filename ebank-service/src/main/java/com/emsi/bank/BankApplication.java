package com.emsi.bank;

import com.emsi.common.ApiExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@Import(ApiExceptionHandler.class)
@EnableFeignClients
@SpringBootApplication
public class BankApplication {
    public static void main(String[] args) { SpringApplication.run(BankApplication.class, args); }
}

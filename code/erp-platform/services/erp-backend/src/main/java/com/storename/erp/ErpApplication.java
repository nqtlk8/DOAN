package com.storename.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.retry.annotation.EnableRetry
@org.springframework.data.jpa.repository.config.EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }

}

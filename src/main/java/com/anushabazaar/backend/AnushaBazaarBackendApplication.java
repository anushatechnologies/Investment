package com.anushabazaar.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AnushaBazaarBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnushaBazaarBackendApplication.class, args);
    }
}

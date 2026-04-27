package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.example", "procrastination_alg"})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
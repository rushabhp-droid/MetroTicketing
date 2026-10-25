package com.mumbaimetro;

import com.mumbaimetro.data.MetroDatabaseManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class MetroApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetroApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Initialize SQLite database on startup (preserving original behavior)
        MetroDatabaseManager.initializeDatabase();
        System.out.println(">>> Mumbai Metro Ticketing System - Spring Boot Web Application Started");
        System.out.println(">>> Access at: http://localhost:8080");
    }
}

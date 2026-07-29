package com.veterinaria.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class VeterinariaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(VeterinariaBackendApplication.class, args);
    }
}
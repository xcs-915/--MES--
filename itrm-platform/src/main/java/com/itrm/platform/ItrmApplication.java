package com.itrm.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ItrmApplication {
    public static void main(String[] args) {
        SpringApplication.run(ItrmApplication.class, args);
    }
}

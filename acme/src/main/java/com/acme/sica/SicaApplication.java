package com.acme.sica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SicaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SicaApplication.class, args);
    }
}

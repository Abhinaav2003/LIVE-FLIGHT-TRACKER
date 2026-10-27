package com.example.planetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PlaneTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlaneTrackerApplication.class, args);
    }
}
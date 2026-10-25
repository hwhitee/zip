package com.wardsentinel.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WardSentinelApplication {

    public static void main(String[] args) {
        SpringApplication.run(WardSentinelApplication.class, args);
    }
}

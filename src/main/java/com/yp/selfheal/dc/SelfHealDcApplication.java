package com.yp.selfheal.dc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SelfHealDcApplication {
    public static void main(String[] args) {
        SpringApplication.run(SelfHealDcApplication.class, args);
    }
}

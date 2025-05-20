package com.lucene_to_elastic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClitestApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClitestApplication.class, args);
    }

}

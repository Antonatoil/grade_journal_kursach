package com.example.grade_journal_back;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        log.info("Starting grade journal backend application");
        SpringApplication.run(Main.class, args);
        log.info("Grade journal backend application started successfully");
    }
}
package com.sudh.accord;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// @EnableScheduling added for WeeklyNarrativeScheduler's Sunday 9am cron job
// (the Gemini narrative feature) — no other @Scheduled jobs existed before this.
@SpringBootApplication
@EnableScheduling
@RestController
public class AccordBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccordBackendApplication.class, args);
    }
}
package com.example.sleeprism;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableJpaAuditing
@SpringBootApplication
@EnableAsync
public class SleeprismApplication {

	public static void main(String[] args) {
		SpringApplication.run(SleeprismApplication.class, args);
		System.out.println("Started....");
	}

}

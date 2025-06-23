package com.example.sleeprism;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SleeprismApplication {

	public static void main(String[] args) {
		SpringApplication.run(SleeprismApplication.class, args);
		System.out.println("Started....");
	}

}

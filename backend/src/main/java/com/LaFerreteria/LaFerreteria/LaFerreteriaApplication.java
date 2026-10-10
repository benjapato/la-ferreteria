package com.LaFerreteria.LaFerreteria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LaFerreteriaApplication {

	public static void main(String[] args) {
		SpringApplication.run(LaFerreteriaApplication.class, args);
	}

}

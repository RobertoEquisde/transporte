package com.adavec.transporte;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransporteApplication {

	public static void main(String[] args) {
		SpringApplication.run(TransporteApplication.class, args);
	}
	@PostConstruct
	public void printEnv() {
		System.out.println("==== VARIABLES DE ENTORNO ====");
		System.out.println("DB_HOST: " + System.getenv("DB_HOST"));
		System.out.println("DB_PORT: " + System.getenv("DB_PORT"));
		System.out.println("DB_NAME: " + System.getenv("DB_NAME"));
		System.out.println("DB_USER: " + System.getenv("DB_USER"));
		System.out.println("DB_PASSWORD: " + System.getenv("DB_PASSWORD"));
	}

}

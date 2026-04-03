package com.matchvagas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class BackendApplication {
	private static final Logger logger = LoggerFactory.getLogger(BackendApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
		Dotenv dotenv = Dotenv.load();
		if (dotenv != null) {
			String exampleVar = dotenv.get("EXAMPLE_VAR");
			if (exampleVar != null) {
				logger.info("Variável de ambiente EXAMPLE_VAR: {}", exampleVar);
			}
		}
	}

}

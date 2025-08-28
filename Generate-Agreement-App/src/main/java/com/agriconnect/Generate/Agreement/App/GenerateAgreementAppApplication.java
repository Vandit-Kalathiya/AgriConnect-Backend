package com.agriconnect.Generate.Agreement.App;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

public class GenerateAgreementAppApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().directory("D:\\VK18\\My Projects\\AgriConnect\\Backend\\Generate-Agreement-App\\.env").load();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		SpringApplication.run(GenerateAgreementAppApplication.class, args);
	}

}

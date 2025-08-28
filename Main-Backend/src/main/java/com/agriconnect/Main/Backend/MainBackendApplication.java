package com.agriconnect.Main.Backend;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class MainBackendApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().directory("D:\\VK18\\My Projects\\AgriConnect\\Backend\\Main-Backend\\.env").load();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		SpringApplication.run(MainBackendApplication.class, args);
	}
}

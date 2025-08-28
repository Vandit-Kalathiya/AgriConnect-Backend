package com.agriconnect.Market.Access.App;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MarketAccessAppApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().directory("D:\\VK18\\My Projects\\AgriConnect\\Backend\\Market-Access-App\\.env").load();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		SpringApplication.run(MarketAccessAppApplication.class, args);
	}

}

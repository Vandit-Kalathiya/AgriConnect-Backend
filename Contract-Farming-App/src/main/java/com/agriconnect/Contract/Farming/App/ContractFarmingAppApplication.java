package com.agriconnect.Contract.Farming.App;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.io.File;

@SpringBootApplication
@EnableDiscoveryClient
public class ContractFarmingAppApplication {

	public static void main(String[] args) {
		String currentDir = System.getProperty("user.dir") + File.separator
				+ "Contract-Farming-App";
		System.out.println("Current Directory: " + currentDir);
		Dotenv dotenv = Dotenv.configure().directory(currentDir).load();
		System.out.println(dotenv.entries());
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		SpringApplication.run(ContractFarmingAppApplication.class, args);
	}

}

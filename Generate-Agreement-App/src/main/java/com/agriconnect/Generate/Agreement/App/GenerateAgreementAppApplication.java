package com.agriconnect.Generate.Agreement.App;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GenerateAgreementAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(GenerateAgreementAppApplication.class, args);
	}
}
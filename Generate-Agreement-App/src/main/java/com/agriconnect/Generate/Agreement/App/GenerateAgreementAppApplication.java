package com.agriconnect.Generate.Agreement.App;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableRetry
@EnableScheduling
public class GenerateAgreementAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(GenerateAgreementAppApplication.class, args);
	}
}
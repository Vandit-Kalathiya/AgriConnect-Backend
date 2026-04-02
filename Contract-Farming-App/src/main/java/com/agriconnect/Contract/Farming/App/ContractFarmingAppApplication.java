package com.agriconnect.Contract.Farming.App;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableRetry
@EnableScheduling
public class ContractFarmingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContractFarmingAppApplication.class, args);
	}
}
package com.agriconnect.Contract.Farming.App;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ContractFarmingAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContractFarmingAppApplication.class, args);
	}
}
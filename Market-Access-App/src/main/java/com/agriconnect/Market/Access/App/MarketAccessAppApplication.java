package com.agriconnect.Market.Access.App;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class MarketAccessAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarketAccessAppApplication.class, args);
	}
}
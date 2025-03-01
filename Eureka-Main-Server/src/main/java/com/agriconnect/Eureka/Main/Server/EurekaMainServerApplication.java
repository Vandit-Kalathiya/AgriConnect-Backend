package com.agriconnect.Eureka.Main.Server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaMainServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(EurekaMainServerApplication.class, args);
	}

}

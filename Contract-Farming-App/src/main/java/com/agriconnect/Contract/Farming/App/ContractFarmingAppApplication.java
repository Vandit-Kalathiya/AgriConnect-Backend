package com.agriconnect.Contract.Farming.App;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@EnableDiscoveryClient
public class ContractFarmingAppApplication {

	public static void main(String[] args) {
		loadEnvironmentVariables();
		SpringApplication.run(ContractFarmingAppApplication.class, args);
	}

	private static void loadEnvironmentVariables() {
		try {
			// Get the application's working directory
			String projectRoot = System.getProperty("user.dir");
			String envPath = projectRoot;

			// Check if we're running from a subdirectory (like Contract-Farming-App)
			if (!Files.exists(Paths.get(projectRoot, ".env"))) {
				// Look for a .env file in the current module directory
				String moduleName = "Contract-Farming-App";
				if (projectRoot.endsWith(moduleName)) {
					envPath = projectRoot;
				} else {
					envPath = projectRoot + File.separator + moduleName;
				}
			}

			// Load .env file if it exists
			File envFile = new File(envPath, ".env");
			if (envFile.exists()) {
				Dotenv dotenv = Dotenv.configure()
						.directory(envPath)
						.ignoreIfMalformed()
						.ignoreIfMissing()
						.load();

				// Set system properties
				dotenv.entries().forEach(entry ->
						System.setProperty(entry.getKey(), entry.getValue())
				);

				System.out.println("Loaded environment variables from: " + envFile.getAbsolutePath());
			} else {
				System.out.println("No .env file found at: " + envFile.getAbsolutePath());
				System.out.println("Please create a .env file based on .env.example");
			}

		} catch (Exception e) {
			System.err.println("Error loading environment variables: " + e.getMessage());
			// Don't fail the application startup, just log the error
		}
	}
}
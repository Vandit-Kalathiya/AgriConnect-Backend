package com.agriconnect.Contract.Farming.App.BlockChainConfig;

//import com.agriconnect.Contract.Farming.App.AgreementRegistry.AgreementRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.math.BigInteger;
import java.util.List;

@Configuration
public class BlockChainConfig {

    private static final String API_URL = System.getProperty("c");

    private static final String PRIVATE_KEY = System.getProperty("PRIVATE_KEY");

    private static final String CONTRACT_ADDRESS = System.getProperty("CONTRACT_ADDRESS");


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);

        // Enable CORS
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.authorizeHttpRequests(authorize -> {
            authorize.anyRequest().permitAll();
        });

        return http.build();
    }

//    @Bean
//    public Web3j web3j() {
//        return Web3j.build(new HttpService(API_URL));
//    }
//
//    @Bean
//    public Credentials credentials() {
//        return Credentials.create(PRIVATE_KEY);
//    }
//
//    @Bean
//    public ContractGasProvider gasProvider() {
//        // Set a reasonable gas limit below the block gas limit (e.g., 500,000)
//        BigInteger gasLimit = BigInteger.valueOf(500_000);
//        BigInteger gasPrice = BigInteger.valueOf(20_000_000_000L);
//        return new StaticGasProvider(gasPrice, gasLimit);
//    }
//
//    @Bean
//    public AgreementRegistry agreementRegistry(Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
//        return AgreementRegistry.load(
//                CONTRACT_ADDRESS,
//                web3j,
//                credentials,
//                gasProvider
//        );
//    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5000","http://localhost:5174")); // Frontend URL
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

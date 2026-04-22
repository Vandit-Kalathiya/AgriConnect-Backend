package com.agriconnect.api.gateway.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({ JwtProperties.class, TwilioProperties.class })
public class ApplicationConfig {

        @Bean
        @LoadBalanced
        public RestClient.Builder loadBalancedRestClientBuilder() {
                PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
                cm.setMaxTotal(200);
                cm.setDefaultMaxPerRoute(50);
                cm.setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(5))
                                .build());

                CloseableHttpClient httpClient = HttpClients.custom()
                                .setConnectionManager(cm)
                                .setDefaultRequestConfig(RequestConfig.custom()
                                                .setResponseTimeout(Timeout.ofSeconds(10))
                                                .build())
                                .build();

                return RestClient.builder()
                                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        }

        @Bean
        public RestClient restClient(RestClient.Builder builder) {
                return builder.build();
        }
}

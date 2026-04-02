package com.agriconnect.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials-path:firebase-service-account.json}")
    private String credentialsPath;

    /**
     * Initialises Firebase Admin SDK from a service-account JSON file.
     * If the credentials file is absent (e.g. local dev without FCM), the push
     * dispatcher degrades gracefully and logs a warning instead of crashing.
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        InputStream credStream;
        try {
            credStream = new FileInputStream(credentialsPath);
        } catch (IOException ex) {
            log.warn("[FIREBASE] Credentials file '{}' not found — push notifications disabled.", credentialsPath);
            return null;
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credStream))
                .build();

        return FirebaseApp.initializeApp(options);
    }
}

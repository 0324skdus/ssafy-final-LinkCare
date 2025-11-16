package com.ssafy.linkcare.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FCMConfig {

    @Value("${firebase.credentials-path}")
    private Resource credentialsPath;

    @Value("${firebase.credentials-json:}")
    private String credentialsJson;

    @Value("${firebase.credentials-b64:}")
    private String credentialsB64;


    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount;
            if (credentialsB64 != null && !credentialsB64.isEmpty()) {
                log.info("Firebase 초기화: Base64 환경 변수 사용 (배포)");
                byte[] bytes = java.util.Base64.getDecoder().decode(credentialsB64);
                serviceAccount = new ByteArrayInputStream(bytes);
            } else if (credentialsJson != null && !credentialsJson.isEmpty()) {
                log.info("Firebase 초기화: JSON 문자열 환경 변수 사용 (배포)");
                serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes());
            } else {
                log.info("Firebase 초기화: 파일 경로 사용 (로컬)");
                serviceAccount = credentialsPath.getInputStream();
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK 초기화 완료");
            }

        } catch (IOException e) {
            log.error("Firebase 초기화 실패", e);
            throw new RuntimeException("Firebase 초기화 실패", e);
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        return FirebaseMessaging.getInstance();
    }
}

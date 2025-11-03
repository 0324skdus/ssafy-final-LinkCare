package com.ssafy.linkcare.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${firebase.credentials-json}")
    private String credentialsJson;

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount;

            // 환경 변수에 JSON 문자열이 있으면 사용 (배포 환경)
            if (credentialsJson != null && !credentialsJson.isEmpty()) {
                log.info("Firebase 초기화: 환경 변수 JSON 사용 (배포 환경)");
                serviceAccount = new ByteArrayInputStream(credentialsJson.getBytes());
            } else {
                // 파일 경로로 초기화 (로컬 개발 환경)
                log.info("Firebase 초기화: 파일 경로 사용 (로컬 환경)");
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
}

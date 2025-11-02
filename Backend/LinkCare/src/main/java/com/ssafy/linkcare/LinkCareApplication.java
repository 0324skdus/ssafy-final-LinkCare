package com.ssafy.linkcare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class LinkCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkCareApplication.class, args);
    }

}

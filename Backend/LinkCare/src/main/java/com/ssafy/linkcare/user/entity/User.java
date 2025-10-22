package com.ssafy.linkcare.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_pk")
    private Long userPk;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    private Float height;

    private Float weight;

    private LocalDate birth;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public User(String email, String password, String name, String imageUrl, Float height, Float weight, LocalDate birth) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.imageUrl = imageUrl;
        this.height = height;
        this.weight = weight;
        this.birth = birth;
    }

    // 비밀번호 변경
    public void updatePassword(String password) {
        this.password = password;
    }

    // 프로필 정보 수정
    public void updateProfile(String name, String imageUrl, Float height, Float weight, LocalDate birth) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.height = height;
        this.weight = weight;
        this.birth = birth;
    }

}

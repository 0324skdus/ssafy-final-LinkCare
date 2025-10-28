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

    @Column(nullable = true)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    private Float height;

    private Float weight;

    private LocalDate birth;

    @Column(length = 10)
    private String gender;

    // 소셜 로그인 필드
    @Column(length = 20)
    private String provider;  // GOOGLE, KAKAO, LOCAL

    @Column(name = "provider_id", length = 100)
    private String providerId;  // 구글 / 카카오 고유 ID

    // 운동 시작 년도
    @Column(name = "exercise_start_year")
    private Integer exerciseStartYear;  // 예: 2020

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public User(String email, String password, String name, String imageUrl, Float height, Float weight, LocalDate birth, String gender, String provider, String providerId, Integer exerciseStartYear) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.imageUrl = imageUrl;
        this.height = height;
        this.weight = weight;
        this.birth = birth;
        this.gender = gender;
        this.provider = provider;
        this.providerId = providerId;
        this.exerciseStartYear = exerciseStartYear;
    }

    // 비밀번호 변경
    public void updatePassword(String password) {
        this.password = password;
    }

    // 프로필 정보 수정 (운동 시작 년도 포함!)
    public void updateProfile(String name, String imageUrl, Float height, Float weight, LocalDate birth, String gender, Integer exerciseStartYear) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.height = height;
        this.weight = weight;
        this.birth = birth;
        this.gender = gender;
        this.exerciseStartYear = exerciseStartYear;
    }

}

package com.ssafy.linkcare.user.entity;

import com.ssafy.linkcare.background.entity.UserBackground;
import com.ssafy.linkcare.character.entity.UserCharacter;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter @Setter
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

    @Column(length = 50)
    private String petName;

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

    // FCM 토큰 (푸시 알림용)
    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_character_id")
    private UserCharacter mainCharacter;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCharacter> characters = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_background_id")
    private UserBackground mainBackground;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserBackground> backgrounds = new ArrayList<>();

    @Builder
    public User(String email, String password, String name, Float height, Float weight, LocalDate birth, String gender, String provider, String providerId, Integer exerciseStartYear) {
        this.email = email;
        this.password = password;
        this.name = name;
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
    public void updateProfile(String name, Float height, Float weight, LocalDate birth, String gender, Integer exerciseStartYear, String petName) {
        this.name = name;
        this.height = height;
        this.weight = weight;
        this.birth = birth;
        this.gender = gender;
        this.exerciseStartYear = exerciseStartYear;
        this.petName = petName;
    }

    // FCM 토큰 업데이트
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateLastSyncTime() {
        this.lastSyncTime = LocalDateTime.now();
    }

}

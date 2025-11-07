package com.ssafy.linkcare.user.entity;

import com.ssafy.linkcare.character.entity.UserCharacter;
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
import java.util.ArrayList;
import java.util.List;

/*
    * 사용자 엔티티

    * 역할: LinkCare 서비스의 사용자 정보를 저장

    * 로그인 방식:
        * - LOCAL: 이메일/비밀번호 (password 필수)
        * - GOOGLE: 구글 소셜 로그인 (providerId에 구글 고유 ID 저장)
        * - KAKAO: 카카오 소셜 로그인 (providerId에 카카오 고유 ID 저장)

    * 주요 기능:
        * - 그룹 참여: GroupMember를 통해 여러 그룹에 소속 가능
        * - 알림 수신: FCM 토큰으로 푸시 알림 받기
        * - 프로필 관리: 신체 정보, 운동 시작 년도 등

    * 필수 필드:
        * - email: 로그인 ID (unique)
        * - name: 사용자 이름
        * - provider: 로그인 방식 (GOOGLE, KAKAO, LOCAL)

    * 선택 필드:
        * - password: LOCAL 로그인만 사용
        * - height, weight, birth, gender: 프로필 정보
        * - exerciseStartYear: 운동 시작 년도 (통계용)
        * - fcmToken: 푸시 알림 수신용 토큰

    * 연관 엔티티:
        * - GroupMember (1:N) - 사용자가 속한 그룹들
        * - GroupJoinRequest (1:N) - 사용자가 신청한 참가 신청들
        * - Notification (1:N) - 사용자가 받은 알림들
*/
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

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "main_character_id")
    private UserCharacter mainCharacter;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCharacter> characters = new ArrayList<>();

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
    public void updateProfile(String name, Float height, Float weight, LocalDate birth, String gender, Integer exerciseStartYear) {
        this.name = name;
        this.height = height;
        this.weight = weight;
        this.birth = birth;
        this.gender = gender;
        this.exerciseStartYear = exerciseStartYear;
    }

    // FCM 토큰 업데이트
    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void updateMainCharacter(UserCharacter userCharacter) {
        this.mainCharacter = userCharacter;
    }
}

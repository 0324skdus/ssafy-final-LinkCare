package com.ssafy.linkcare.user.repository;

import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    /*
     *Google OAuth2용 메서드 추가
        * 소셜 로그인 사용자 조회
        * @param provider "GOOGLE", "KAKAO", "LOCAL" 등
        * @param providerId 소셜 플랫폼에서 제공하는 고유 ID
        * @return User 엔티티
    */
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    /**
     * 동일한 FCM 토큰을 쓰고 있는 "다른 유저"들의 토큰을 전부 null 로 초기화
     * -> 하나의 FCM 토큰은 항상 한 유저만 갖도록 보장
     */
    @Modifying
    @Query("UPDATE User u " +
            "SET u.fcmToken = null " +
            "WHERE u.fcmToken = :token AND u.userPk <> :userId")
    void clearFcmTokenFromOtherUsers(@Param("token") String token,
                                     @Param("userId") Long userId);
}

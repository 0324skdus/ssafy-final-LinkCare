package com.ssafy.linkcare.user.repository;

import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
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
}

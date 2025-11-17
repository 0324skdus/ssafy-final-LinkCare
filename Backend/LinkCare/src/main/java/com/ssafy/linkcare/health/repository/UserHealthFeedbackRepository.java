package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.UserHealthFeedback;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserHealthFeedbackRepository extends JpaRepository<UserHealthFeedback, Integer> {

    // 사용자의 피드백 조회 (1:1이므로 하나만 존재)
    Optional<UserHealthFeedback> findByUser(User user);

    // 존재 여부 확인
    boolean existsByUser(User user);
}
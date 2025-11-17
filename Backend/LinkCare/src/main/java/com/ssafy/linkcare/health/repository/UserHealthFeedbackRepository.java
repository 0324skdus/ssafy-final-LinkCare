package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.UserHealthFeedback;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserHealthFeedbackRepository extends JpaRepository<UserHealthFeedback, Integer> {

    // 사용자의 피드백 조회 (1:1이므로 하나만 존재)
    Optional<UserHealthFeedback> findByUser(User user);

    // 존재 여부 확인
    boolean existsByUser(User user);

    // 오늘 날짜에 생성된 피드백만 조회
    @Query("SELECT f FROM UserHealthFeedback f WHERE f.user = :user " +
            "AND DATE(f.createdAt) = CURRENT_DATE")
    Optional<UserHealthFeedback> findTodayFeedbackByUser(@Param("user") User user);

    @Query("SELECT f FROM UserHealthFeedback f WHERE f.user = :user " +
            "AND f.createdAt >= :startOfDay AND f.createdAt < :endOfDay")
    Optional<UserHealthFeedback> findByUserAndDate(
            @Param("user") User user,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}

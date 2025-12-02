package com.ssafy.linkcare.background.repository;

import com.ssafy.linkcare.background.entity.UserBackground;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBackgroundRepository extends JpaRepository<UserBackground, Long> {
    Optional<UserBackground> findByUserAndUserBackgroundId(User user, Long userBackgroundId);
}

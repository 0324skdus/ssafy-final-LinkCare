package com.ssafy.linkcare.background.repository;

import com.ssafy.linkcare.background.entity.UserBackground;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBackgroundRepository extends JpaRepository<UserBackground, Long> {
}

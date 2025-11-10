package com.ssafy.linkcare.point.repository;

import com.ssafy.linkcare.point.entity.Point;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PointRepository extends JpaRepository<Point, Long> {
    Optional<Point> findByUser(User user);
}

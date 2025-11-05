package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.ActivitySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivitySummaryRepository extends JpaRepository<ActivitySummary, Long> {

}

package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.BloodPressure;
import com.ssafy.linkcare.health.entity.Exercise;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Integer> {

    Optional<Exercise> findByUserAndUid(User user, String uid);

    @Query("SELECT e FROM Exercise e WHERE e.user = :user " +
            "AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', e.startTime)) = :startDate")
    List<Exercise> findByUserAndStartDate(@Param("user") User user,
                                               @Param("startDate") LocalDate startDate);

    @Modifying
    @Query("DELETE FROM Exercise e WHERE e.user = :user AND e.uid IN :uids")
    void deleteByUserAndUidIn(@Param("user") User user, @Param("uids") Set<String> uids);
}

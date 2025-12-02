package com.ssafy.linkcare.alarm.repository;

import com.ssafy.linkcare.alarm.entity.Alarm;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    List<Alarm> findByReceiverAndIsReadOrderBySentAtDesc(User receiver, boolean isRead);

    List<Alarm> findByReceiverOrderBySentAtDesc(User receiver);
}

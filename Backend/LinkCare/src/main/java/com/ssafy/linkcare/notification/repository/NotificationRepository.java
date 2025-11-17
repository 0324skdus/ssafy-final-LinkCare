package com.ssafy.linkcare.notification.repository;

import com.ssafy.linkcare.notification.entity.Notification;
import com.ssafy.linkcare.notification.enums.NotificationType;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자의 모든 알림 조회 (최신순)
    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    // 사용자의 특정 카테고리 알림 조회 (최신순)
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.type IN :types ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndTypeInOrderByCreatedAtDesc(
            @Param("user") User user,
            @Param("types") List<NotificationType> types
    );

    // 안 읽은 알림 개수 조회
    Long countByUserAndIsReadFalse(User user);

    // 특정 카테고리의 안 읽은 알림 개수 조회
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false AND n.type IN :types")
    Long countByUserAndIsReadFalseAndTypeIn(
            @Param("user") User user,
            @Param("types") List<NotificationType> types
    );

    // 특정 그룹의 모든 알림 조회
    @Query("SELECT n FROM Notification n WHERE n.relatedGroup.groupSeq = :groupSeq")
    List<Notification> findByRelatedGroupSeq(@Param("groupSeq") Long groupSeq);
}

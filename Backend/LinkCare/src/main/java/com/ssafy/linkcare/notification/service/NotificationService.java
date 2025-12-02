package com.ssafy.linkcare.notification.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupJoinRequest;
import com.ssafy.linkcare.notification.dto.NotificationResponse;
import com.ssafy.linkcare.notification.dto.UnreadCountResponse;
import com.ssafy.linkcare.notification.entity.Notification;
import com.ssafy.linkcare.notification.enums.NotificationType;
import com.ssafy.linkcare.notification.fcm.FCMService;
import com.ssafy.linkcare.notification.repository.NotificationRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final FCMService fcmService;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    /*
        * 알림 생성 (내부용 - GroupService에서 호출)
    */
    @Transactional
    public void createNotification(User user, NotificationType type, String title, String content, Group relatedGroup, GroupJoinRequest relatedRequest) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .content(content)
                .relatedGroup(relatedGroup)
                .relatedRequest(relatedRequest)
                .build();

        notificationRepository.save(notification);

        log.info("알림 생성 완료: userId={}, type={}, title={}", user.getUserPk(), type, title);

        // FCM 푸시 알림 전송 (그룹 타입 정보 포함)
        String groupType = relatedGroup != null ? relatedGroup.getType().name() : null;
        fcmService.sendPushNotificationWithData(user, title, content, type, groupType);
    }

    /*
        * 내 알림 목록 조회 (카테고리 필터링)
        * @param category "ALL" 또는 "GROUP"
    */
    public List<NotificationResponse> getMyNotifications(Long userId, String category) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Notification> notifications;

        if ("GROUP".equalsIgnoreCase(category)) {
            // 그룹 카테고리 알림만 조회
            List<NotificationType> groupTypes = List.of(
                    NotificationType.GROUP_JOIN_REQUEST,
                    NotificationType.GROUP_JOIN_APPROVED,
                    NotificationType.GROUP_JOIN_REJECTED,
                    NotificationType.GROUP_PERMISSION_CHANGED
            );
            notifications = notificationRepository.findByUserAndTypeInOrderByCreatedAtDesc(user, groupTypes);
        } else {
            // 전체 알림 조회 (ALL)
            notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        }

        log.info("알림 목록 조회: userId={}, category={}, count={}", userId, category, notifications.size());

        return notifications.stream()
                .map(this::toNotificationResponse)
                .toList();
    }

    /*
        * 안 읽은 알림 개수 조회
    */
    public UnreadCountResponse getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 전체 안 읽은 알림 개수
        Long totalCount = notificationRepository.countByUserAndIsReadFalse(user);

        // 그룹 카테고리 안 읽은 알림 개수
        List<NotificationType> groupTypes = List.of(
                NotificationType.GROUP_JOIN_REQUEST,
                NotificationType.GROUP_JOIN_APPROVED,
                NotificationType.GROUP_JOIN_REJECTED,
                NotificationType.GROUP_PERMISSION_CHANGED
        );
        Long groupCount = notificationRepository.countByUserAndIsReadFalseAndTypeIn(user, groupTypes);

        log.info("안 읽은 알림 개수 조회: userId={}, total={}, group={}", userId, totalCount, groupCount);

        return new UnreadCountResponse(totalCount, groupCount);
    }

    /*
        * 알림 읽음 처리
    */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getUser().getUserPk().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인의 알림만 읽음 처리할 수 있습니다");
        }

        notification.markAsRead();

        log.info("알림 읽음 처리: userId={}, notificationId={}", userId, notificationId);
    }

    /*
        * 전체 알림 읽음 처리
    */
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Notification> unreadNotifications = notificationRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .filter(n -> !n.getIsRead())
                .toList();

        unreadNotifications.forEach(Notification::markAsRead);

        log.info("전체 알림 읽음 처리: userId={}, count={}", userId, unreadNotifications.size());
    }

    /*
        * 알림 삭제
    */
    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 본인의 알림인지 확인
        if (!notification.getUser().getUserPk().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인의 알림만 삭제할 수 있습니다");
        }

        notificationRepository.delete(notification);

        log.info("알림 삭제: userId={}, notificationId={}", userId, notificationId);
    }

    /*
        * Notification -> NotificationResponse 변환
    */
    private NotificationResponse toNotificationResponse(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRelatedGroup() != null ? notification.getRelatedGroup().getGroupSeq() : null,
                notification.getRelatedRequest() != null ? notification.getRelatedRequest().getRequestSeq() : null,
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}

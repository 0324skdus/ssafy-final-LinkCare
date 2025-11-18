package com.ssafy.linkcare.notification.fcm;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.ssafy.linkcare.notification.enums.NotificationType;
import com.ssafy.linkcare.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FCMService {

    /*
        * FCM 푸시 알림 전송
            * @param user 알림 받을 사용자
            * @param title 알림 제목
            * @param body 알림 내용
    */
    public void sendPushNotification(User user, String title, String body) {
        sendPushNotificationWithData(user, title, body, null, null);
    }

    /*
        * FCM 푸시 알림 전송 (데이터 포함)
            * @param user 알림 받을 사용자
            * @param title 알림 제목
            * @param body 알림 내용
            * @param notificationType 알림 타입
            * @param groupType 그룹 타입 (CARE 또는 HEALTH)
    */
    public void sendPushNotificationWithData(User user, String title, String body, NotificationType notificationType, String groupType) {
        log.info("FCM 푸시 알림 전송 시도: userId={}, title={}, fcmToken={}",
                user.getUserPk(), title, user.getFcmToken() != null ? "있음" : "없음");

        // FCM 토큰이 없으면 전송하지 않음
        if (user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
            log.warn("FCM 토큰이 없어 푸시 알림 전송 불가: userId={}", user.getUserPk());
            return;
        }

        try {
            // 데이터 페이로드 구성
            Map<String, String> data = new HashMap<>();
            if (notificationType != null) {
                data.put("type", notificationType.name());
            }
            if (groupType != null) {
                data.put("groupType", groupType);
            }
            if (title != null) {
                data.put("title", title);
            }
            if (body != null) {
                data.put("body", body);
            }

            data.put("userId", String.valueOf(user.getUserPk()));

            // FCM 메시지 생성
            Message message = Message.builder()
                    .setToken(user.getFcmToken())
                    .putAllData(data)
                    .build();

            // FCM 전송
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("FCM 푸시 알림 전송 성공: userId={}, type={}, groupType={}, response={}",
                    user.getUserPk(), notificationType, groupType, response);

        } catch (Exception e) {
            log.error("FCM 푸시 알림 전송 실패: userId={}, error={}", user.getUserPk(), e.getMessage());
        }
    }
}

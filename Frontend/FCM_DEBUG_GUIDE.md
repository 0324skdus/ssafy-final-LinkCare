# FCM 에뮬레이터 디버깅 가이드

## 1. 에뮬레이터 요구사항 확인

### 에뮬레이터가 Google Play Services를 지원하는지 확인
- AVD Manager에서 에뮬레이터 이미지가 **"Google APIs" 또는 "Google Play"** 버전인지 확인
- ❌ "AOSP" 이미지는 FCM 지원 안 함
- ✅ "Google APIs" 또는 "Google Play" 이미지만 FCM 지원

### Google Play Services 버전 확인
1. 에뮬레이터 실행
2. Settings > Apps > Show system apps
3. "Google Play services" 앱 찾기
4. 버전이 최신인지 확인 (업데이트 필요하면 Play Store에서 업데이트)

## 2. 로그캣에서 확인해야 할 로그

### MainActivity FCM 토큰 업로드 로그 (앱 시작 시)
```
I/MainActivity: === MainActivity FCM 토큰 업데이트 시작 ===
I/MainActivity: 액세스 토큰 확인 완료
I/MainActivity: 현재 FCM 토큰 획득: dxxx...
D/MainActivity: 서버로 FCM 토큰 전송 중...
I/MainActivity: ✅ FCM 토큰 서버 전송 성공!
```

**만약 에러가 나면:**
- `액세스 토큰이 없어 FCM 토큰 전송 스킵` → 로그인 먼저 해야 함
- `FCM 토큰 서버 전송 실패` → 네트워크 연결 문제 또는 서버 문제

### FCM 메시지 수신 로그 (푸시 받을 때)
```
D/FCMService: 푸시 알림 수신: gcm.googleapis.com
D/FCMService: 알림 데이터: {type=GROUP_JOIN_REQUEST, groupType=HEALTH}
D/FCMService: 알림 제목: 그룹 이름
D/FCMService: 알림 내용: 닉네임님이 가입 신청을 했습니다.
D/FCMService: 새 알림 브로드캐스트 전송
```

**이 로그가 안 나오면:**
- FCM 토큰이 서버에 제대로 저장 안 됨
- 또는 Google Play Services 문제

## 3. 백엔드 로그 확인

### FCM 토큰 저장 로그
```
INFO: FCM 토큰 업데이트 요청: userId=4, userName=테스트, 토큰길이=163
INFO: ✅ FCM 토큰 업데이트 완료: userId=4, userName=테스트
```

### FCM 전송 로그
```
INFO: FCM 푸시 알림 전송 시도: userId=4, title=그룹명, fcmToken=있음
INFO: FCM 푸시 알림 전송 성공: userId=4, type=GROUP_JOIN_REQUEST, groupType=HEALTH, response=projects/linkcare-68f20/messages/0:xxx
```

## 4. 알림 권한 확인 (Android 13+)

### 앱에서 알림 권한 요청했는지 확인
- 앱 설치 후 첫 실행 시 "알림 허용" 팝업이 나와야 함
- Settings > Apps > LinkCare > Notifications가 **켜짐**인지 확인

### 권한 확인 코드 (임시로 추가해서 테스트)
MainActivity.kt의 onCreate에 추가:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED) {
        Log.w("MainActivity", "❌ 알림 권한 없음!")
        requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
    } else {
        Log.i("MainActivity", "✅ 알림 권한 있음")
    }
}
```

## 5. FCM 토큰 직접 확인

### 현재 FCM 토큰 출력
Logcat에서 검색: `현재 FCM 토큰 획득`
```
I/MainActivity: 현재 FCM 토큰 획득: dxxx...
```

### 백엔드 DB에 저장된 토큰 확인
백엔드 서버에서 SQL 실행:
```sql
SELECT user_pk, name, fcm_token FROM user WHERE user_pk = 4;
```

**두 토큰이 일치하는지 확인!**
- 다르면: 토큰 업로드가 실패했거나 다른 기기의 토큰임
- 같으면: FCM 수신 문제

## 6. 테스트 방법

### 1단계: 간단한 FCM 테스트 메시지 보내기
Firebase Console에서 직접 테스트:
1. Firebase Console → Cloud Messaging → Send test message
2. FCM 토큰 입력 (Logcat에서 복사)
3. Send → 에뮬레이터에서 알림 오는지 확인

**이게 오면:** 앱 FCM 설정은 정상, 백엔드 연동 문제
**이것도 안 오면:** 에뮬레이터 Google Play Services 문제

### 2단계: 백엔드에서 실제 FCM 전송
포스트맨으로 그룹 참가 신청:
1. 다른 계정으로 로그인
2. 그룹 참가 신청 POST
3. 방장 계정 에뮬레이터에서 알림 오는지 확인

## 7. 일반적인 해결 방법

### 방법 1: 에뮬레이터 재시작
- 에뮬레이터 종료 후 다시 시작
- Cold Boot 시도 (AVD Manager → Wipe Data)

### 방법 2: Google Play Services 업데이트
- 에뮬레이터 Play Store 열기
- "Google Play services" 검색
- 업데이트

### 방법 3: 새 에뮬레이터 생성
- AVD Manager에서 새 기기 생성
- **반드시 "Google Play" 이미지 선택**
- API Level 30 이상 권장

### 방법 4: 실제 기기에서 테스트
- 에뮬레이터가 계속 문제면 실제 Android 기기로 테스트
- USB 디버깅 연결
- 앱 설치 후 테스트

## 8. 체크리스트

- [ ] 에뮬레이터가 Google Play 이미지인가?
- [ ] Google Play Services가 설치되어 있는가?
- [ ] 앱에서 알림 권한이 허용되었는가? (Android 13+)
- [ ] MainActivity 로그에서 "FCM 토큰 서버 전송 성공" 나오는가?
- [ ] 백엔드 로그에서 "FCM 토큰 업데이트 완료" 나오는가?
- [ ] 백엔드 DB의 FCM 토큰과 앱의 FCM 토큰이 일치하는가?
- [ ] 백엔드 로그에서 "FCM 푸시 알림 전송 성공" 나오는가?
- [ ] Firebase Console에서 직접 보낸 테스트 메시지는 오는가?

## 9. 다음 단계

위 체크리스트를 모두 확인한 후:

1. **Firebase Console 테스트가 성공하면:**
   - 백엔드 FCM 전송 로직 문제
   - FCM 토큰이 DB에 잘못 저장됨

2. **Firebase Console 테스트도 실패하면:**
   - 에뮬레이터 문제
   - Google Play Services 문제
   - 실제 기기에서 테스트 필요

3. **모든 로그는 정상인데 알림 안 오면:**
   - MyFirebaseMessagingService.kt의 onMessageReceived가 호출 안 됨
   - AndroidManifest.xml의 서비스 등록 확인

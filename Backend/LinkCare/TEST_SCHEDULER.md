# 주간 목표 달성 스케줄러 테스트 가이드

## 🎯 테스트 목적
`WeeklyGoalRewardScheduler`가 정상적으로 목표 달성을 체크하고 포인트를 지급하는지 확인

---

## 📋 사전 준비

### 1. 테스트 데이터 준비

#### Step 1: 그룹 생성
```sql
-- 그룹이 이미 있다면 기존 그룹 사용 가능
SELECT * FROM `groups` LIMIT 1;
```

#### Step 2: 그룹 멤버 확인
```sql
-- 그룹 seq=1의 멤버 확인
SELECT gm.*, u.name, u.email
FROM group_members gm
JOIN users u ON gm.user_seq = u.user_pk
WHERE gm.group_seq = 1;
```

#### Step 3: 지난 주 월요일 계산
```javascript
// 오늘이 2025-11-19 (화요일)이면
// 이번 주 월요일: 2025-11-18
// 지난 주 월요일: 2025-11-11
const today = new Date();
const dayOfWeek = today.getDay();
const thisMonday = new Date(today);
thisMonday.setDate(today.getDate() - (dayOfWeek === 0 ? 6 : dayOfWeek - 1));
const lastMonday = new Date(thisMonday);
lastMonday.setDate(thisMonday.getDate() - 7);

console.log("지난 주 월요일:", lastMonday.toISOString().split('T')[0]);
```

#### Step 4: 테스트 목표 데이터 삽입
```sql
-- 지난 주 목표 삽입 (2025-11-11 기준)
INSERT INTO weekly_group_goals (
    group_seq,
    week_start,
    goal_steps,
    goal_kcal,
    goal_duration,
    goal_distance,
    predicted_growth_rate_steps,
    predicted_growth_rate_kcal,
    predicted_growth_rate_duration,
    predicted_growth_rate_distance,
    selected_metric_type,  -- ⭐ 중요!
    created_at
) VALUES (
    1,                      -- 그룹 seq
    '2025-11-11',          -- 지난 주 월요일
    10000,                 -- 목표 걸음수
    500.0,                 -- 목표 칼로리
    300,                   -- 목표 운동시간(분)
    7.0,                   -- 목표 이동거리(km)
    1.05,
    1.05,
    1.05,
    1.05,
    'STEPS',               -- 걸음수로 측정
    UNIX_TIMESTAMP()
);
```

#### Step 5: 테스트 통계 데이터 삽입

**케이스 1: 목표 달성 (실제 > 목표)**
```sql
INSERT INTO weekly_group_stats (
    group_seq,
    week_start,
    week_end,
    member_count,
    avg_age,
    avg_bmi,
    group_steps_total,     -- 12000 > 10000 ✅ 달성!
    group_kcal_total,
    group_duration_total,
    group_distance_total,
    member_steps_var,
    created_at
) VALUES (
    1,
    '2025-11-11',
    '2025-11-17',
    5,
    30.0,
    22.5,
    12000,                 -- ✅ 목표(10000)보다 많음
    450.0,
    280,
    6.5,
    1500.0,
    UNIX_TIMESTAMP()
);
```

**케이스 2: 목표 미달성 (실제 < 목표)**
```sql
INSERT INTO weekly_group_stats (
    group_seq,
    week_start,
    week_end,
    member_count,
    avg_age,
    avg_bmi,
    group_steps_total,     -- 8000 < 10000 ❌ 미달성
    group_kcal_total,
    group_duration_total,
    group_distance_total,
    member_steps_var,
    created_at
) VALUES (
    1,
    '2025-11-11',
    '2025-11-17',
    5,
    30.0,
    22.5,
    8000,                  -- ❌ 목표(10000)보다 적음
    450.0,
    280,
    6.5,
    1500.0,
    UNIX_TIMESTAMP()
);
```

#### Step 6: 멤버 포인트 현황 확인 (실행 전)
```sql
SELECT
    u.user_pk,
    u.name,
    p.balance as current_points
FROM group_members gm
JOIN users u ON gm.user_seq = u.user_pk
LEFT JOIN points p ON p.user_pk = u.user_pk
WHERE gm.group_seq = 1;
```

---

## 🧪 테스트 방법

### 방법 1: **API 수동 트리거** (추천 👍)

#### 1. 서버 실행
```bash
cd c:\Users\SSAFY\Desktop\S13P31A307\Backend\LinkCare
./gradlew bootRun
```

#### 2. API 호출 (Postman/curl/Thunder Client)
```bash
POST http://localhost:9090/api/test/schedulers/weekly-goal-reward
```

**예상 응답:**
```json
{
  "message": "주간 목표 달성 체크 완료",
  "success": true
}
```

#### 3. 로그 확인
콘솔에서 다음과 같은 로그 확인:
```
========================================
=== 주간 목표 달성 체크 및 포인트 지급 시작 ===
========================================
체크 대상 주차: 2025-11-11
전체 그룹 수: 1개

그룹 1 - 🎉 목표 달성! (타입: STEPS, 목표: 10000, 실제: 12000)
  - 사용자 1 에게 10포인트 지급
  - 사용자 2 에게 10포인트 지급
  ...
그룹 1 - 총 5명에게 50포인트 지급 완료

========================================
=== 주간 목표 달성 체크 및 포인트 지급 완료 ===
체크 완료: 1개
  - 목표 달성: 1개 (50포인트 지급)
  - 목표 미달성: 0개
  - 목표 미설정: 0개
  - 통계 데이터 없음: 0개
========================================
```

#### 4. 포인트 증가 확인
```sql
SELECT
    u.user_pk,
    u.name,
    p.balance as current_points
FROM group_members gm
JOIN users u ON gm.user_seq = u.user_pk
LEFT JOIN points p ON p.user_pk = u.user_pk
WHERE gm.group_seq = 1;

-- 각 멤버의 포인트가 +10 증가했는지 확인!
```

---

### 방법 2: **cron 표현식 변경** (1분마다 자동 실행)

#### 1. 스케줄러 수정
```java
// WeeklyGoalRewardScheduler.java 44번째 줄
@Scheduled(cron = "0 * * * * *")  // 1분마다 실행 (원래: 0 5 0 * * MON)
```

#### 2. 서버 재시작
```bash
./gradlew bootRun
```

#### 3. 1분 대기 후 로그 확인
자동으로 스케줄러가 실행되는지 확인

⚠️ **주의**: 테스트 완료 후 원래 cron으로 복원!
```java
@Scheduled(cron = "0 5 0 * * MON")  // 매주 월요일 0시 5분
```

---

## ✅ 테스트 체크리스트

### 시나리오 1: 목표 달성 (정상 케이스)
- [ ] 지난 주 목표 데이터 존재
- [ ] `selectedMetricType` 설정됨
- [ ] 지난 주 통계 데이터 존재
- [ ] 실제 활동량 ≥ 목표값
- [ ] **예상 결과**: 모든 멤버 +10포인트

### 시나리오 2: 목표 미달성
- [ ] 실제 활동량 < 목표값
- [ ] **예상 결과**: 포인트 지급 없음, 로그만 기록

### 시나리오 3: 목표 미설정
- [ ] `selectedMetricType` = null
- [ ] **예상 결과**: 스킵, 로그: "메트릭 타입 미선택"

### 시나리오 4: 통계 데이터 없음
- [ ] `weekly_group_stats`에 해당 주차 데이터 없음
- [ ] **예상 결과**: 스킵, 로그: "통계 데이터 없음"

### 시나리오 5: 여러 그룹
- [ ] 그룹 A: 목표 달성 → 포인트 지급
- [ ] 그룹 B: 목표 미달성 → 포인트 지급 없음
- [ ] **예상 결과**: 각 그룹 독립적으로 처리

---

## 🐛 트러블슈팅

### 문제 1: "포인트 정보를 찾을 수 없습니다"
**원인**: 멤버의 `points` 테이블 레코드 없음
**해결**:
```sql
INSERT INTO points (user_pk, balance)
SELECT user_pk, 0 FROM users WHERE user_pk NOT IN (SELECT user_pk FROM points);
```

### 문제 2: "지난 주 목표 없음"
**원인**: `week_start`가 지난 주 월요일과 일치하지 않음
**해결**: `week_start` 날짜 다시 계산 후 데이터 삽입

### 문제 3: 스케줄러가 실행 안됨
**원인**: `@EnableScheduling` 설정 누락
**해결**:
```java
// Application.java 또는 Config 클래스
@EnableScheduling
public class LinkCareApplication { ... }
```

---

## 🧹 테스트 데이터 정리

테스트 완료 후:
```sql
-- 테스트 목표 삭제
DELETE FROM weekly_group_goals WHERE week_start = '2025-11-11';

-- 테스트 통계 삭제
DELETE FROM weekly_group_stats WHERE week_start = '2025-11-11';

-- 포인트 복원 (선택)
UPDATE points SET balance = balance - 10 WHERE user_pk IN (...);
```

---

## 📌 실제 운영 전 체크사항

1. ✅ cron 표현식이 `0 5 0 * * MON`인지 확인
2. ✅ 테스트 컨트롤러 제거 또는 관리자 권한 추가
3. ✅ 포인트 지급 금액 확인 (현재: 10포인트)
4. ✅ 로그 레벨 적절히 조정 (debug → info)
5. ✅ DB 백업 완료

---

## 🎁 추가 기능 아이디어

- [ ] 목표 달성 시 푸시 알림 전송
- [ ] 달성률 통계 저장 (group_goal_records 활용)
- [ ] 연속 달성 시 보너스 포인트
- [ ] 그룹 순위 시스템

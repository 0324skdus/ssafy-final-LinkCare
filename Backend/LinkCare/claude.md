# AI 기반 그룹 목표 생성 기능 구현 문서

## 개요
FastAPI AI 서버의 LightGBM 모델을 활용하여 그룹의 주간 건강 목표(걸음수, 칼로리, 운동시간, 이동거리)를 자동으로 생성하는 기능입니다.

---

## 🏗️ 아키텍처

```
[Client]
   ↓
[GroupController] → POST /api/groups/{groupSeq}/goals?requestDate=2024-01-10
   ↓
[GoalGenerationService]
   ↓ (1) 요청 날짜 → 이번 주 월요일 계산
   ↓ (2) 저번 주부터 과거 최대 3주 데이터 조회
   ↓ (3) 통계 계산 (평균, 표준편차)
   ↓ (4) AI 서버 호출 (4개 모델)
[AiClient] → FastAPI
   ↓ (5) 성장률 기반 주간 목표 계산
   ↓ (6) DB 저장 (week_start 기준 월~일 목표)
[WeeklyGroupGoals]
```

---

## 📊 데이터 흐름

### 1. 날짜 처리 로직

#### 예시: 수요일에 목표 생성
```
요청: 2024-01-10 (수요일)

계산:
- 이번 주 월요일: 2024-01-08
- 저번 주 월요일: 2024-01-01

사용 데이터 (저번 주부터 과거로):
  Week -1: 2024-01-01(월) ~ 2024-01-07(일)  ← 저번 주
  Week -2: 2023-12-25(월) ~ 2023-12-31(일)  ← 저저번 주
  Week -3: 2023-12-18(월) ~ 2023-12-24(일)  ← 저저저번 주

✅ 이번 주(2024-01-08~) 데이터는 제외!

저장:
- week_start: 2024-01-08
- goal_steps: 10,500 (주간 전체 목표)
```

#### 특징:
- ✅ **어떤 요일이든 가능**: 월요일 체크 제거
- ✅ **자동 월요일 계산**: 요청 날짜 → 해당 주 월요일
- ✅ **유연한 데이터 수집**: 최대 3주, 1주만 있어도 OK
- ✅ **이번 주 제외**: 저번 주부터 과거 데이터만 사용

---

### 2. 입력 데이터 (저번 주부터 최대 3주)
**DB 테이블**: `weekly_group_stats`

| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `week_start` | LocalDate | 주 시작일 (월요일) |
| `member_count` | int | 그룹 인원 수 |
| `avg_age` | Float | 그룹 평균 나이 |
| `avg_bmi` | Float | 그룹 평균 BMI |
| `group_steps_total` | Long | 주간 그룹 총 걸음수 |
| `group_kcal_total` | Float | 주간 그룹 총 칼로리 |
| `group_duration_total` | int | 주간 그룹 총 운동시간(분) |
| `group_distance_total` | Float | 주간 그룹 총 이동거리(km) |
| `member_steps_var` | Float | 멤버 간 걸음수 표준편차 ⚠️ |

⚠️ **주의**: 컬럼명은 `var`이지만 실제로는 **표준편차(std)** 값입니다.

---

### 3. AI 모델 입력 Feature (공통 7개)

각 모델(Steps, Kcal, Duration, Distance)은 동일한 구조의 7개 Feature를 입력받습니다:

#### 📌 공통 Feature (3개)
- `member_count`: 그룹 인원 수 (최신 주차)
- `avg_age`: 그룹 평균 나이 (최신 주차)
- `avg_bmi`: 그룹 평균 BMI (최신 주차)

#### 📌 타겟별 Feature (4개)

**Steps 모델**:
- `group_steps_mean_3w`: 과거 데이터 평균 걸음수
- `group_steps_std_3w`: 과거 데이터 걸음수 표준편차
- `group_duration_mean_3w`: 과거 데이터 평균 운동시간
- `member_steps_std`: 멤버 간 걸음수 표준편차

**Kcal 모델**:
- `group_kcal_mean_3w`: 과거 데이터 평균 칼로리
- `group_kcal_std_3w`: 과거 데이터 칼로리 표준편차
- `group_duration_mean_3w`: 과거 데이터 평균 운동시간
- `member_steps_std`: 멤버 간 걸음수 표준편차

**Duration 모델**:
- `group_duration_mean_3w`: 과거 데이터 평균 운동시간
- `group_duration_std_3w`: 과거 데이터 운동시간 표준편차
- `group_steps_mean_3w`: 과거 데이터 평균 걸음수
- `member_steps_std`: 멤버 간 걸음수 표준편차

**Distance 모델**:
- `group_distance_mean_3w`: 과거 데이터 평균 이동거리
- `group_distance_std_3w`: 과거 데이터 이동거리 표준편차
- `group_duration_mean_3w`: 과거 데이터 평균 운동시간
- `member_steps_std`: 멤버 간 걸음수 표준편차

---

### 4. AI 모델 출력
**응답 형식** (공통):
```json
{
  "predicted_growth_rate": 1.05
}
```

- `predicted_growth_rate`: 성장률 (예: 1.05 = 5% 증가)

---

### 5. 목표 계산 로직
```java
주간 목표 = 과거 데이터 평균값 × 성장률

예시:
- 과거 평균 걸음수: 10,000보
- AI 예측 성장률: 1.05
- 이번 주 목표: 10,000 × 1.05 = 10,500보
```

**남은 목표 계산** (프론트엔드):
```javascript
// 수요일에 조회했을 때
const remainingDays = 5; // 수~일
const dailyGoal = weeklyGoal * (remainingDays / 7);
// 10,500 × (5/7) = 7,500보
```

---

## 📁 구현된 파일 구조

```
src/main/java/com/ssafy/linkcare/
├── ai/
│   ├── client/
│   │   └── AiClient.java                    # FastAPI 통신 클라이언트
│   ├── dto/
│   │   ├── StepsPredictRequest.java         # Steps 예측 요청 DTO
│   │   ├── KcalPredictRequest.java          # Kcal 예측 요청 DTO
│   │   ├── DurationPredictRequest.java      # Duration 예측 요청 DTO
│   │   ├── DistancePredictRequest.java      # Distance 예측 요청 DTO
│   │   └── AiPredictResponse.java           # AI 응답 DTO (공통)
│   └── service/
│       └── GoalGenerationService.java       # 목표 생성 핵심 서비스
├── group/
│   ├── entity/
│   │   ├── WeeklyGroupStats.java            # 주간 통계 엔티티 (기존)
│   │   └── WeeklyGroupGoals.java            # 주간 목표 엔티티 (신규)
│   ├── repository/
│   │   ├── WeeklyGroupStatsRepository.java  # 통계 조회 (메서드 추가)
│   │   └── WeeklyGroupGoalsRepository.java  # 목표 저장 (신규)
│   ├── dto/
│   │   └── WeeklyGroupGoalResponse.java     # 목표 응답 DTO (신규)
│   └── controller/
│       └── GroupController.java             # API 엔드포인트 추가
└── config/
    └── WebClientConfig.java                 # AI 서버용 WebClient 빈 추가
```

---

## 🔧 주요 구현 내용

### 1. **AiClient** - FastAPI 통신

```java
@Service
public class AiClient {
    @Qualifier("aiWebClient")
    private final WebClient aiWebClient;

    public AiPredictResponse predictSteps(StepsPredictRequest request) {
        return aiWebClient.post()                    // POST 요청
                .uri("/predict/steps")                // 엔드포인트
                .bodyValue(request)                   // JSON 본문
                .retrieve()                           // 응답 받기
                .bodyToMono(AiPredictResponse.class) // 객체 변환
                .block();                             // 동기 대기
    }
}
```

#### WebClient 각 줄 설명:
1. `.post()`: POST 요청 시작
2. `.uri()`: 경로 설정 (baseUrl + uri)
3. `.bodyValue()`: 요청 본문 (Java 객체 → JSON)
4. `.retrieve()`: 응답 수신 선언
5. `.bodyToMono()`: 응답 본문 → Java 객체 변환 (비동기)
6. `.block()`: 비동기 → 동기 변환 (응답 대기)

---

### 2. **GoalGenerationService** - 핵심 비즈니스 로직

#### 📌 처리 흐름
```java
@Transactional
public WeeklyGroupGoals generateNextWeekGoal(Long groupSeq, LocalDate requestDate) {
    // 1. 이번 주 월요일 계산
    LocalDate weekStart = requestDate.with(DayOfWeek.MONDAY);

    // 2. 저번 주 이전 데이터만 필터링
    List<WeeklyGroupStats> pastWeeks = allWeeks.stream()
        .filter(stats -> stats.getWeekStart().isBefore(weekStart))
        .toList();

    // 3. 통계 계산 (평균, 표준편차)
    // 4. AI 호출 (4개 모델)
    // 5. 목표 계산 = 평균 × 성장률
    // 6. DB 저장 (week_start 기준)
}
```

#### 📌 표준편차 계산
```java
private double calculateStandardDeviation(List<Double> values) {
    double mean = values.stream().mapToDouble(v -> v).average().orElse(0.0);

    double variance = values.stream()
        .mapToDouble(v -> Math.pow(v - mean, 2))
        .average()
        .orElse(0.0);

    return Math.sqrt(variance);  // ✅ 표준편차 = √분산
}
```

---

### 3. **API 엔드포인트**

#### 📌 요청
```http
POST /api/groups/{groupSeq}/goals?requestDate=2024-01-10
```

**파라미터**:
- `groupSeq` (Path): 그룹 시퀀스
- `requestDate` (Query): 요청 날짜 (어떤 요일이든 가능)

#### 📌 응답 (200 OK)
```json
{
  "weeklyGroupGoalsSeq": 1,
  "groupSeq": 123,
  "weekStart": "2024-01-08",
  "goalSteps": 10500,
  "goalKcal": 525.0,
  "goalDuration": 315,
  "goalDistance": 7.35,
  "predictedGrowthRateSteps": 1.05,
  "predictedGrowthRateKcal": 1.05,
  "predictedGrowthRateDuration": 1.05,
  "predictedGrowthRateDistance": 1.05
}
```

#### 📌 에러 응답
- `404`: 그룹을 찾을 수 없음
- `409`: 과거 통계 데이터가 존재하지 않음

---

## ⚙️ 설정 파일

### application.properties
```properties
# AI FastAPI Server
ai.server.url=http://ai-server:8000
```

⚠️ **주의**: `ai-server`는 Docker 컨테이너 이름입니다. 실제 환경에 맞게 수정하세요.
- 로컬 테스트: `http://localhost:8000`
- 실제 서버: 실제 IP/도메인

### WebClientConfig
```java
@Bean(name = "aiWebClient")
public WebClient aiWebClient() {
    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)  // 연결 타임아웃 10초
        .responseTimeout(Duration.ofSeconds(10));              // 응답 타임아웃 10초

    return WebClient.builder()
        .baseUrl(aiServerUrl)  // application.properties에서 주입
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
}
```

**역할**: HTTP 클라이언트 설정 관리
- 타임아웃 설정
- Base URL 지정
- Spring Bean으로 등록 → 의존성 주입

---

## 🚨 주의사항

### 1. **표준편차 vs 분산**
- ❌ **문서/컬럼명**: "분산(variance)"이라고 표기
- ✅ **실제 계산**: **표준편차(std)** 사용
- **이유**: Python 전처리 코드에서 `np.std()` 사용

### 2. **FastAPI 엔드포인트**
AI 서버는 다음 4개 엔드포인트를 제공해야 합니다:
- `POST /predict/steps`
- `POST /predict/kcal`
- `POST /predict/duration`
- `POST /predict/distance`

### 3. **데이터 요구사항**
- **최소**: 1주 데이터 (저번 주)
- **권장**: 3주 데이터 (저번 주 포함)
- **이번 주 데이터는 제외**

### 4. **날짜 처리**
- ✅ 어떤 요일이든 가능
- ✅ 자동으로 해당 주 월요일 계산
- ✅ 저번 주부터 과거 데이터만 사용

### 5. **남은 목표 계산**
- DB에는 주간 전체 목표 저장
- 남은 일수 계산은 **프론트엔드**에서 처리
- 예: 수요일 → 5/7 비율 계산

---

## 📊 DB 스키마

### weekly_group_goals (신규 테이블)
```sql
CREATE TABLE `weekly_group_goals` (
    `weekly_group_goals_seq` INT AUTO_INCREMENT PRIMARY KEY,
    `group_seq` BIGINT NOT NULL,
    `week_start` DATE NOT NULL,
    `goal_steps` BIGINT NOT NULL,
    `goal_kcal` FLOAT NOT NULL,
    `goal_duration` INT NOT NULL,
    `goal_distance` FLOAT NOT NULL,
    `predicted_growth_rate_steps` DOUBLE NOT NULL,
    `predicted_growth_rate_kcal` DOUBLE NOT NULL,
    `predicted_growth_rate_duration` DOUBLE NOT NULL,
    `predicted_growth_rate_distance` DOUBLE NOT NULL,
    `created_at` BIGINT,
    FOREIGN KEY (`group_seq`) REFERENCES `groups`(`group_seq`)
);
```

#### 주요 특징:
- ✅ **PK는 INT**: 충분한 범위 (42억)
- ✅ **FK는 BIGINT**: Group 테이블과 타입 통일
- ✅ **week_end 제거**: 계산 가능 (week_start + 6일)
- ✅ **is_completed 없음**: group_goal_records 테이블에서 관리

### group_goal_records (달성 기록)
```sql
CREATE TABLE `group_goal_records` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `group_seq` BIGINT NOT NULL,
    `week_start` DATE NOT NULL,
    `goal_steps` BIGINT,
    `actual_steps` BIGINT,
    `achievement_rate_steps` FLOAT,
    `goal_kcal` FLOAT,
    `actual_kcal` FLOAT,
    `achievement_rate_kcal` FLOAT,
    `goal_duration` INT,
    `actual_duration` INT,
    `achievement_rate_duration` FLOAT,
    `goal_distance` FLOAT,
    `actual_distance` FLOAT,
    `achievement_rate_distance` FLOAT,
    `is_succeeded` BOOLEAN,
    `created_at` TIMESTAMP,
    FOREIGN KEY (`group_seq`) REFERENCES `groups`(`group_seq`)
);
```

---

## 🔍 사용 예시

### 1. 정상 케이스 (수요일 요청)
```bash
# 요청 (2024-01-10 수요일)
curl -X POST "http://localhost:9090/api/groups/1/goals?requestDate=2024-01-10"

# 응답
{
  "weeklyGroupGoalsSeq": 1,
  "groupSeq": 1,
  "weekStart": "2024-01-08",   // 이번 주 월요일
  "goalSteps": 10500,           // 주간 전체 목표
  "goalKcal": 525.0,
  "goalDuration": 315,
  "goalDistance": 7.35,
  "predictedGrowthRateSteps": 1.05,
  "predictedGrowthRateKcal": 1.05,
  "predictedGrowthRateDuration": 1.05,
  "predictedGrowthRateDistance": 1.05
}
```

### 2. 프론트엔드 남은 목표 계산
```javascript
const response = await fetch('/api/groups/1/goals?requestDate=2024-01-10');
const data = await response.json();

// 수요일이면 남은 일수 = 5일 (수, 목, 금, 토, 일)
const today = new Date('2024-01-10');
const weekStart = new Date(data.weekStart);
const weekEnd = new Date(weekStart);
weekEnd.setDate(weekEnd.getDate() + 6);

const totalDays = 7;
const remainingDays = Math.ceil((weekEnd - today) / (1000 * 60 * 60 * 24)) + 1;

const remainingGoal = {
  steps: Math.round(data.goalSteps * (remainingDays / totalDays)),  // 7,500
  kcal: data.goalKcal * (remainingDays / totalDays),
  duration: Math.round(data.goalDuration * (remainingDays / totalDays)),
  distance: data.goalDistance * (remainingDays / totalDays)
};
```

### 3. 에러 케이스 - 데이터 없음
```bash
# 요청 (과거 데이터 없는 신규 그룹)
curl -X POST "http://localhost:9090/api/groups/999/goals?requestDate=2024-01-10"

# 응답 (409)
{
  "message": "데이터가 부족합니다. 최근 3주 데이터가 필요합니다"
}
```

---

## 🎯 향후 개선 가능 사항

1. **병렬 처리**: 4개 AI 모델 호출을 병렬화하여 성능 개선 (Mono.zip)
2. **캐싱**: 동일 주차 목표 재생성 방지
3. **재시도 로직**: AI 서버 장애 시 재시도 (WebClient retry)
4. **모니터링**: AI 응답 시간 추적
5. **배치 작업**: 매주 일요일 자동 목표 생성

---

## 📝 FAQ

### Q1: 왜 AiClient를 별도로 만들었나요?
**A**: 외부 API 통신은 별도 클래스로 분리하는 것이 관례입니다.
- **Controller**: 사용자 요청 처리
- **Service**: 비즈니스 로직
- **Client**: 외부 API 통신

### Q2: WebClientConfig는 왜 필요한가요?
**A**: HTTP 클라이언트 설정을 중앙화하기 위함입니다.
- 타임아웃 설정
- Base URL 지정
- 재사용 가능한 Bean 등록

### Q3: INT vs BIGINT PK?
**A**:
- **INT**: 42억까지 (주간 목표는 충분)
- **BIGINT**: 920경까지 (overkill)
- **FK는 부모와 동일 타입 유지** (group_seq는 BIGINT)

### Q4: 남은 목표는 왜 프론트에서 계산하나요?
**A**:
- DB에는 주간 전체 목표만 저장
- 남은 일수는 조회 시점마다 다름
- 프론트에서 실시간 계산이 효율적

---

## 📝 변경 이력

- **2024-XX-XX**: 초기 구현 완료
  - AI 기반 그룹 목표 생성 기능
  - 4가지 메트릭 예측 (Steps, Kcal, Duration, Distance)
  - 표준편차 기반 통계 계산
  - 유연한 날짜 처리 (어떤 요일이든 가능)
  - 저번 주부터 최대 3주 데이터 사용

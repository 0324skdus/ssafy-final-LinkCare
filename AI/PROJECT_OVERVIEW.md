# LinkCare AI 프로젝트 전체 개요

> **작성일**: 2025-01-11
> **버전**: v6 (정책 기반 사전 학습)
> **상태**: 개발 완료, 배포 준비 중

---

## 📋 목차

1. [프로젝트 소개](#1-프로젝트-소개)
2. [전체 시스템 아키텍처](#2-전체-시스템-아키텍처)
3. [데이터 파이프라인](#3-데이터-파이프라인)
4. [머신러닝 모델](#4-머신러닝-모델)
5. [API 서비스](#5-api-서비스)
6. [데이터베이스 스키마](#6-데이터베이스-스키마)
7. [백엔드 통합 플로우](#7-백엔드-통합-플로우)
8. [향후 발전 계획](#8-향후-발전-계획)

---

## 1. 프로젝트 소개

### 1.1 프로젝트 개요

**LinkCare AI**는 그룹 단위의 헬스케어 목표를 자동으로 생성하는 머신러닝 서비스입니다.

#### 핵심 목표
- 그룹의 **최근 3주 운동 데이터**를 기반으로 다음 주의 **적정 목표 성장률** 예측
- **4개 메트릭** 독립 예측: 걸음수, 운동시간, 칼로리, 이동거리
- **정책 기반 사전 학습** 방식으로 실제 달성 데이터 없이도 작동 가능

#### 비즈니스 가치
- ✅ **Cold Start 해결**: 신규 그룹도 즉시 목표 생성 가능
- ✅ **과학적 목표 설정**: 규칙 기반이 아닌 머신러닝 기반 추천
- ✅ **확장 가능성**: 실제 데이터 누적 후 fine-tuning 가능

---

### 1.2 기술 스택

| 구분 | 기술 |
|------|------|
| **언어** | Python 3.12 |
| **ML 프레임워크** | LightGBM (Gradient Boosting) |
| **API 서버** | FastAPI 0.110.0 |
| **데이터 처리** | Pandas, NumPy, Jupyter Notebook |
| **배포** | Docker, AWS EC2 (예정) |
| **통합** | Spring Boot 백엔드 (Kotlin) + Android 앱 |

---

## 2. 전체 시스템 아키텍처

### 2.1 AI 서버 내부 구조

```
AI Server (FastAPI)
├── data/
│   ├── raw/
│   │   └── health_fitness_dataset.csv          # 원본 데이터
│   └── processed/
│       ├── training_steps.csv                  # 걸음수 학습 데이터 (8 컬럼)
│       ├── training_duration.csv               # 운동시간 학습 데이터 (8 컬럼)
│       ├── training_kcal.csv                   # 칼로리 학습 데이터 (8 컬럼)
│       └── training_distance.csv               # 거리 학습 데이터 (8 컬럼)
│
├── notebooks/
│   ├── 01_data_exploration.ipynb               # 데이터 탐색
│   ├── 02_feature_engineering.ipynb            # 학습 데이터 생성 ⭐
│   └── 03_model_training.ipynb                 # 모델 학습 ⭐
│
├── models/
│   ├── goal_steps_model.txt                    # 걸음수 예측 모델
│   ├── goal_duration_model.txt                 # 운동시간 예측 모델
│   ├── goal_kcal_model.txt                     # 칼로리 예측 모델
│   └── goal_distance_model.txt                 # 거리 예측 모델
│
├── app/
│   ├── main.py                                 # FastAPI 엔드포인트
│   ├── predict.py                              # 모델 로드 및 예측 로직
│   └── schemas.py                              # Pydantic 스키마
│
└── requirements.txt                            # Python 의존성
```

---

## 3. 데이터 파이프라인

### 3.1 전체 플로우

```
원본 데이터 (CSV)
    ↓
[1] 데이터 정제 및 그룹 생성
    ↓
[2] 개인별 주간 집계
    ↓
[3] 그룹별 주간 집계
    ↓
[4] 3주 이동 통계 계산 (핵심!)
    ↓
[5] 정책 기반 타겟 생성
    ↓
학습용 CSV 4개 (Steps, Duration, Kcal, Distance)
    ↓
[6] LightGBM 모델 학습
    ↓
모델 파일 4개 (.txt)
    ↓
[7] FastAPI 서버에 로드
    ↓
[8] 백엔드가 API 호출
```

### 3.2 데이터 전처리 단계별 설명

#### [1] 데이터 정제 및 그룹 생성
**파일**: `notebooks/02_feature_engineering.ipynb` Part 1-2

**작업 내용**:
- 불필요한 컬럼 제거 (건강 상태, 혈압, 흡연 등)
- 유지 컬럼: `participant_id`, `date`, `age`, `bmi`, `daily_steps`, `calories_burned`, `duration_minutes`, `fitness_level`
- 파생 변수 생성: `distance_km = daily_steps * 0.00075` (평균 보폭 0.75m)
- **그룹 생성 전략**:
  - `fitness_level` 기준으로 참가자 정렬
  - 랜덤 크기 (2-6명) 그룹 생성
  - 유사한 운동 수준의 사람들이 같은 그룹에 배치

**출력**: 그룹 ID가 부여된 개인 일별 데이터

---

#### [2] 개인별 주간 집계
**파일**: `notebooks/02_feature_engineering.ipynb` Part 3

**작업 내용**:
- 일별 데이터 → 개인별 주간 집계 (월요일 시작)
- 집계 항목:
  - `weekly_steps` = SUM(daily_steps)
  - `weekly_duration` = SUM(duration_minutes)
  - `weekly_kcal` = SUM(calories_burned)
  - `weekly_distance` = SUM(distance_km)

**출력**: 개인별 주간 운동량 데이터

---

#### [3] 그룹별 주간 집계
**파일**: `notebooks/02_feature_engineering.ipynb` Part 4

**작업 내용**:
- 개인별 주간 집계 → 그룹별 주간 평균/편차
- 집계 항목:
  - `member_count`: 그룹 인원 수
  - `avg_age`, `avg_bmi`: 평균 나이, BMI
  - `group_steps_total`: 그룹 총 걸음수 (주간)
  - `group_duration_total`: 그룹 총 운동시간 (주간)
  - `group_kcal_total`: 그룹 총 칼로리 (주간)
  - `group_distance_total`: 그룹 총 거리 (주간)
  - `member_steps_var`: 구성원 간 걸음수 편차 (std)

**출력**: 그룹별 주간 통계

---

#### [4] 3주 이동 통계 계산 (핵심!)
**파일**: `notebooks/02_feature_engineering.ipynb` Part 5

**작업 내용**:
- **최근 3주 평균 계산** (Rolling Mean):
  - `group_steps_mean_3w = rolling(3).mean()`
  - `group_duration_mean_3w = rolling(3).mean()`
  - `group_kcal_mean_3w = rolling(3).mean()`
  - `group_distance_mean_3w = rolling(3).mean()`

- **최근 3주 변동성 계산** (Rolling Std):
  - `group_steps_var_3w = rolling(3).std()`
  - `group_duration_var_3w = rolling(3).std()`
  - `group_kcal_var_3w = rolling(3).std()`
  - `group_distance_var_3w = rolling(3).std()`

**중요**: 최소 3주 데이터가 있는 행만 학습 데이터로 사용

**출력**: 7개 Feature가 포함된 데이터

---

#### [5] 정책 기반 타겟 생성
**파일**: `notebooks/02_feature_engineering.ipynb` Part 7

**작업 내용**:
각 메트릭별로 **권장 성장률(recommended_growth_rate)** 계산

**정책 수식**:
```python
# 추세 계산 (최근 3주가 상승세인가?)
trend_3w = (week_3 - week_1) / 2.0

# 수준 계산 (현재 운동량이 높은가?)
level_3w = mean_3w

# 안정성 계산 (변동이 적은가?)
stability_3w = -var_3w

# 정규화 후 가중 합산
signal = (
    0.4 * normalize(trend_3w) +      # 상승세 → 목표 상향
    0.3 * normalize(level_3w) -      # 높은 수준 → 목표 완화
    0.3 * normalize(stability_3w)    # 변동 큼 → 목표 완화
)

# 최종 권장 성장률 (0.95~1.10)
recommended_growth_rate = clip(1.0 + 0.1 * signal, 0.95, 1.10)
```

**해석**:
- `1.00` = 현재 수준 유지
- `1.05` = 5% 성장 권장 (목표 상향)
- `0.98` = 2% 완화 권장 (목표 하향)

**출력**: 4개 CSV 파일 (각 메트릭별 타겟 포함)

---

#### [6] 모델 학습
**파일**: `notebooks/03_model_training.ipynb`

**작업 내용**:
- LightGBM 회귀 모델 학습 (4개 독립 모델)
- 학습 데이터: `training_*.csv` (7 features + 1 target)
- 검증 데이터: 80:20 split
- 평가 지표: RMSE, R²

**하이퍼파라미터**:
```python
params = {
    'objective': 'regression',
    'metric': 'rmse',
    'learning_rate': 0.05,
    'num_leaves': 31,
    'max_depth': -1,
    'min_data_in_leaf': 20,
    'feature_fraction': 0.8,
    'bagging_fraction': 0.8,
    'bagging_freq': 5,
    'verbose': -1,
    'seed': 42
}
num_boost_round = 200
```

**출력**: `models/goal_*_model.txt` (4개 모델)

---

## 4. 머신러닝 모델

### 4.1 모델 구성

| 모델 파일 | 예측 대상 | 입력 Feature 수 | 출력 범위 |
|----------|-----------|---------------|----------|
| `goal_steps_model.txt` | 걸음수 권장 성장률 | 7개 | 0.95~1.10 |
| `goal_duration_model.txt` | 운동시간 권장 성장률 | 7개 | 0.95~1.10 |
| `goal_kcal_model.txt` | 칼로리 권장 성장률 | 7개 | 0.95~1.10 |
| `goal_distance_model.txt` | 거리 권장 성장률 | 7개 | 0.95~1.10 |

**알고리즘**: LightGBM (Gradient Boosting Decision Tree)

---

### 4.2 입력 Feature (7개)

#### 공통 Feature (3개) - 모든 모델 동일
| Feature | 설명 | 타입 | 예시 |
|---------|------|------|------|
| `member_count` | 그룹 인원 수 | int | 4 |
| `avg_age` | 그룹 평균 나이 | float | 35.2 |
| `avg_bmi` | 그룹 평균 BMI | float | 23.5 |

#### 메트릭별 Feature (4개) - 각 모델마다 다름

**Steps 모델**:
- `group_steps_mean_3w`: 최근 3주 평균 걸음수
- `group_steps_var_3w`: 최근 3주 걸음수 변동성
- `group_duration_mean_3w`: 최근 3주 평균 운동시간 (보조)
- `member_steps_var`: 구성원 간 걸음수 편차

**Kcal 모델**:
- `group_kcal_mean_3w`: 최근 3주 평균 칼로리
- `group_kcal_var_3w`: 최근 3주 칼로리 변동성
- `group_duration_mean_3w`: 최근 3주 평균 운동시간 (보조)
- `member_steps_var`: 구성원 간 걸음수 편차

**Duration 모델**:
- `group_duration_mean_3w`: 최근 3주 평균 운동시간
- `group_duration_var_3w`: 최근 3주 운동시간 변동성
- `group_steps_mean_3w`: 최근 3주 평균 걸음수 (보조)
- `member_steps_var`: 구성원 간 걸음수 편차

**Distance 모델**:
- `group_distance_mean_3w`: 최근 3주 평균 거리
- `group_distance_var_3w`: 최근 3주 거리 변동성
- `group_duration_mean_3w`: 최근 3주 평균 운동시간 (보조)
- `member_steps_var`: 구성원 간 걸음수 편차

---

### 4.3 출력 (타겟)

| 타겟 | 의미 | 범위 | 활용 방식 |
|------|------|------|----------|
| `recommended_growth_rate_steps` | 걸음수 권장 성장률 | 0.95~1.10 | `next_goal = mean_3w * growth_rate` |
| `recommended_growth_rate_duration` | 운동시간 권장 성장률 | 0.95~1.10 | `next_goal = mean_3w * growth_rate` |
| `recommended_growth_rate_kcal` | 칼로리 권장 성장률 | 0.95~1.10 | `next_goal = mean_3w * growth_rate` |
| `recommended_growth_rate_distance` | 거리 권장 성장률 | 0.95~1.10 | `next_goal = mean_3w * growth_rate` |

---

## 5. API 서비스

### 5.1 API 엔드포인트

FastAPI 서버는 다음 5개 엔드포인트를 제공합니다:

```
GET  /health                    # 헬스 체크
POST /predict/steps             # 걸음수 권장 성장률 예측
POST /predict/kcal              # 칼로리 권장 성장률 예측
POST /predict/duration          # 운동시간 권장 성장률 예측
POST /predict/distance          # 거리 권장 성장률 예측
POST /reload_model              # 모델 재로드
```

---

### 5.2 API 요청/응답 예시

#### [1] 걸음수 예측 (POST /predict/steps)

**Request**:
```json
{
  "member_count": 4,
  "avg_age": 35.2,
  "avg_bmi": 23.5,
  "group_steps_mean_3w": 224000.0,
  "group_steps_var_3w": 5000.0,
  "group_duration_mean_3w": 1260.0,
  "member_steps_var": 1200.0
}
```

**Response**:
```json
{
  "predicted_growth_rate": 1.027
}
```

**해석**: 다음 주 걸음수 목표 = `224000 * 1.027 = 230048`

---

#### [2] 칼로리 예측 (POST /predict/kcal)

**Request**:
```json
{
  "member_count": 4,
  "avg_age": 35.2,
  "avg_bmi": 23.5,
  "group_kcal_mean_3w": 8400.0,
  "group_kcal_var_3w": 200.0,
  "group_duration_mean_3w": 1260.0,
  "member_steps_var": 1200.0
}
```

**Response**:
```json
{
  "predicted_growth_rate": 1.024
}
```

---

#### [3] 운동시간 예측 (POST /predict/duration)

**Request**:
```json
{
  "member_count": 4,
  "avg_age": 35.2,
  "avg_bmi": 23.5,
  "group_duration_mean_3w": 1260.0,
  "group_duration_var_3w": 50.0,
  "group_steps_mean_3w": 224000.0,
  "member_steps_var": 1200.0
}
```

**Response**:
```json
{
  "predicted_growth_rate": 1.032
}
```

---

#### [4] 거리 예측 (POST /predict/distance)

**Request**:
```json
{
  "member_count": 4,
  "avg_age": 35.2,
  "avg_bmi": 23.5,
  "group_distance_mean_3w": 168.0,
  "group_distance_var_3w": 3.75,
  "group_duration_mean_3w": 1260.0,
  "member_steps_var": 1200.0
}
```

**Response**:
```json
{
  "predicted_growth_rate": 1.027
}
```

---

### 5.3 모델 로드 메커니즘

**파일**: `app/predict.py`

**특징**:
- `@lru_cache` 데코레이터로 모델 1회 로드 후 캐싱
- Thread-safe 모델 재로드 지원
- 예측 범위 강제 제한 (0.95~1.10)

**코드 구조**:
```python
# 모델 경로 정의
MODEL_PATHS = {
    "steps": "models/goal_steps_model.txt",
    "kcal": "models/goal_kcal_model.txt",
    "duration": "models/goal_duration_model.txt",
    "distance": "models/goal_distance_model.txt"
}

# Feature 순서 정의 (각 모델마다 다름)
FEATURES = {
    "steps": ["member_count", "avg_age", "avg_bmi", ...],
    ...
}

# 캐싱된 모델 로드
@lru_cache(maxsize=1)
def load_models():
    return {key: lgb.Booster(model_file=path)
            for key, path in MODEL_PATHS.items()}

# 예측 함수
def predict_goal(metric: str, payload: dict) -> float:
    model = load_models()[metric]
    X = np.array([[payload[f] for f in FEATURES[metric]]])
    y = model.predict(X)[0]
    return max(0.95, min(1.10, y))  # 범위 제한
```

---

## 6. 데이터베이스 스키마

백엔드(Spring Boot)는 다음 테이블을 사용하여 AI 서버와 통합합니다.

### 6.1 주요 테이블 구조

#### [1] weekly_group_stats (주간 그룹 통계)

**목적**: AI 모델 입력 생성을 위한 사전 집계 테이블

| 컬럼명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `id` | BIGINT (PK) | 레코드 ID | 1 |
| `group_id` | BIGINT (FK) | 그룹 ID | 1 |
| `week_start` | DATE | 주 시작일 (월요일) | 2024-01-08 |
| `week_end` | DATE | 주 종료일 (일요일) | 2024-01-14 |
| `member_count` | INT | 해당 주 인원 수 | 4 |
| `avg_age` | FLOAT | 평균 나이 | 35.2 |
| `avg_bmi` | FLOAT | 평균 BMI | 23.5 |
| `group_steps_total` | BIGINT | 그룹 총 걸음수 | 224000 |
| `group_kcal_total` | FLOAT | 그룹 총 칼로리 | 8400.0 |
| `group_duration_total` | INT | 그룹 총 운동시간 (분) | 1260 |
| `group_distance_total` | FLOAT | 그룹 총 거리 (km) | 168.0 |
| `member_steps_var` | FLOAT | 구성원 간 걸음수 편차 | 1200.0 |
| `created_at` | TIMESTAMP | 생성 시각 | 2024-01-15 00:00:00 |

**집계 주기**: 매주 월요일 00:00 배치 작업

---

#### [2] weekly_group_goals (주간 그룹 목표)

**목적**: AI가 생성한 목표 저장

| 컬럼명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `id` | BIGINT (PK) | 레코드 ID | 1 |
| `group_id` | BIGINT (FK) | 그룹 ID | 1 |
| `week_start` | DATE | 목표 주 시작일 | 2024-01-15 |
| `goal_steps` | BIGINT | 걸음수 목표 | 230000 |
| `goal_kcal` | FLOAT | 칼로리 목표 | 8600.0 |
| `goal_duration` | INT | 운동시간 목표 (분) | 1300 |
| `goal_distance` | FLOAT | 거리 목표 (km) | 172.5 |
| `predicted_growth_rate_steps` | FLOAT | Steps 성장률 | 1.027 |
| `predicted_growth_rate_kcal` | FLOAT | Kcal 성장률 | 1.024 |
| `predicted_growth_rate_duration` | FLOAT | Duration 성장률 | 1.032 |
| `predicted_growth_rate_distance` | FLOAT | Distance 성장률 | 1.027 |
| `is_completed` | BOOLEAN | 목표 주 완료 여부 | FALSE |
| `created_at` | TIMESTAMP | 생성 시각 | 2024-01-14 23:00:00 |

**생성 주기**: 매주 일요일 23:00에 다음 주 목표 생성

---

#### [3] group_goal_records (그룹 목표 달성 기록) - v7용

**목적**: 실제 달성 데이터 누적 (Fine-tuning용)

| 컬럼명 | 타입 | 설명 | 예시 |
|--------|------|------|------|
| `id` | BIGINT (PK) | 레코드 ID | 1 |
| `group_id` | BIGINT (FK) | 그룹 ID | 1 |
| `week_start` | DATE | 목표 주 시작일 | 2024-01-15 |
| `goal_steps` | BIGINT | 목표 걸음수 | 230000 |
| `actual_steps` | BIGINT | 실제 달성 걸음수 | 225000 |
| `achievement_rate_steps` | FLOAT | 달성률 | 0.978 |
| `is_succeeded` | BOOLEAN | 달성 여부 (>=1.0) | FALSE |
| (kcal, duration, distance도 동일 구조) | ... | ... | ... |
| `created_at` | TIMESTAMP | 생성 시각 | 2024-01-22 00:00:00 |

**생성 주기**: 매주 월요일 00:00에 이전 주 달성 기록 생성

---

### 6.2 AI 입력 생성 SQL (백엔드)

**Steps 모델 입력 생성 예시**:

```sql
-- 최근 3주 데이터 조회
WITH recent_3_weeks AS (
    SELECT
        group_id, week_start,
        member_count, avg_age, avg_bmi,
        group_steps_total, group_duration_total, member_steps_var
    FROM weekly_group_stats
    WHERE group_id = :group_id
      AND week_start >= DATE_TRUNC('week', CURRENT_DATE) - INTERVAL '3 weeks'
      AND week_start < DATE_TRUNC('week', CURRENT_DATE)
    ORDER BY week_start DESC
    LIMIT 3
),
rolling_stats AS (
    SELECT
        AVG(group_steps_total) AS group_steps_mean_3w,
        STDDEV(group_steps_total) AS group_steps_var_3w,
        AVG(group_duration_total) AS group_duration_mean_3w
    FROM recent_3_weeks
)
SELECT
    (SELECT member_count FROM recent_3_weeks ORDER BY week_start DESC LIMIT 1) AS member_count,
    (SELECT avg_age FROM recent_3_weeks ORDER BY week_start DESC LIMIT 1) AS avg_age,
    (SELECT avg_bmi FROM recent_3_weeks ORDER BY week_start DESC LIMIT 1) AS avg_bmi,
    rs.group_steps_mean_3w,
    rs.group_steps_var_3w,
    rs.group_duration_mean_3w,
    (SELECT member_steps_var FROM recent_3_weeks ORDER BY week_start DESC LIMIT 1) AS member_steps_var
FROM rolling_stats rs;
```

---

## 7. 백엔드 통합 플로우

### 7.1 주간 목표 생성 플로우

```
[매주 일요일 23:00 - 백엔드 배치 작업]

1. 최근 3주 데이터 조회
   Backend → DB (weekly_group_stats)
   └─► 3주 데이터 반환

2. 3주 롤링 통계 계산
   Backend 내부 로직
   └─► 7개 Feature 생성

3. AI 서버 호출 (4번 병렬)
   Backend → AI Server
   ├─► POST /predict/steps    → growth_rate: 1.027
   ├─► POST /predict/kcal     → growth_rate: 1.024
   ├─► POST /predict/duration → growth_rate: 1.032
   └─► POST /predict/distance → growth_rate: 1.027

4. 목표 계산
   Backend 내부 로직
   ├─► next_goal_steps = 224000 * 1.027 = 230048
   ├─► next_goal_kcal = 8400 * 1.024 = 8602
   ├─► next_goal_duration = 1260 * 1.032 = 1300
   └─► next_goal_distance = 168 * 1.027 = 172.5

5. 목표 저장
   Backend → DB (weekly_group_goals)
   └─► 다음 주 목표 저장 완료
```

---

### 7.2 사용자 앱 플로우

```
[월요일 - 사용자가 앱 열람]

1. 앱에서 이번 주 목표 요청
   Android → Backend
   └─► GET /goals/current

2. 백엔드가 DB에서 목표 조회
   Backend → DB (weekly_group_goals)
   └─► 이번 주 목표 데이터 반환

3. 앱에 목표 표시
   Backend → Android
   └─► {
       "goal_steps": 230048,
       "goal_kcal": 8602,
       "goal_duration": 1300,
       "goal_distance": 172.5
     }
```

---

## 8. 향후 발전 계획

### 8.1 v7: 실데이터 기반 Fine-tuning

**시점**: 실제 사용자 데이터 4주 이상 누적 후

#### 추가 Feature (5개)
각 모델은 **자신의 메트릭 달성률**을 사용:

| Feature | 설명 | 타입 | 예시 (Steps) |
|---------|------|------|--------------|
| `achievement_rate_1w` | 이전 주 달성률 | float | 0.978 |
| `achievement_rate_3w` | 최근 3주 평균 달성률 | float | 0.95 |
| `consecutive_success` | 연속 달성 횟수 | int | 2 |
| `consecutive_failure` | 연속 실패 횟수 | int | 0 |
| `trend_achievement_3w` | 달성률 추세 | float | 0.02 |

**총 Feature**: 7 (v6) + 5 (실데이터) = **12개**

#### 타겟 변경
| 항목 | v6 | v7 |
|------|----|----|
| **타겟명** | `recommended_growth_rate` | `optimal_growth_rate` |
| **범위** | 0.95~1.10 | 0.80~1.30 (더 넓음) |
| **계산** | 정책 수식 | 실제 달성률 기반 회귀 |

#### 학습 방법
- **Warm start 재학습**: v6 모델을 초기값으로 사용
- **Residual 보정**: 예측 오차 학습 후 합산
- **앙상블**: v6 정책 모델 + v7 실데이터 모델 가중 평균

---

### 8.2 배포 로드맵

#### 현재 (v6 개발 완료)
- ✅ 정책 기반 학습 데이터 생성
- ✅ 4개 LightGBM 모델 학습
- ✅ FastAPI 서버 구축
- ✅ Python 3.12 환경 구축

#### 단기 (1-2주)
- ⏳ Docker 이미지 빌드 및 테스트
- ⏳ AWS EC2 인스턴스 설정
- ⏳ 모델 파일 S3 업로드
- ⏳ 컨테이너 배포 및 API 테스트

#### 중기 (1개월)
- ⏳ Spring Boot 백엔드 API 연동
- ⏳ 주간 배치 작업 구현 (목표 생성)
- ⏳ HTTPS 적용 (Nginx + Let's Encrypt)
- ⏳ 모니터링 설정 (CloudWatch)

#### 장기 (3-6개월)
- ⏳ 실제 사용자 데이터 수집 (최소 4주)
- ⏳ v7 Fine-tuning 데이터 생성
- ⏳ v7 모델 재학습 및 A/B 테스트
- ⏳ v7 모델 배포

---

### 8.3 기대 효과

#### v6 (현재)
- ✅ Cold Start 문제 해결
- ✅ 규칙 기반 정책 자동화
- ✅ 즉시 서비스 가능

#### v7 (미래)
- 🎯 실제 달성률 반영
- 🎯 정확도 향상 (RMSE 20-30% 개선 예상)
- 🎯 개인화된 목표 추천
- 🎯 지속적인 모델 개선 (온라인 러닝)

---

## 9. 참고 문서

| 문서명 | 설명 |
|-------|------|
| [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) | 개발 환경 설정 및 실행 가이드 |
| [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) | Docker 및 EC2 배포 가이드 |
| [notebooks/02_feature_engineering.ipynb](notebooks/02_feature_engineering.ipynb) | 학습 데이터 생성 로직 |
| [notebooks/03_model_training.ipynb](notebooks/03_model_training.ipynb) | 모델 학습 및 평가 |
| [app/predict.py](app/predict.py) | 모델 로드 및 예측 로직 |

---

## 10. 문의 및 기여

**프로젝트 위치**: `C:/Users/SSAFY/Desktop/LinkCare/S13P31A307/AI`

**주요 브랜치**:
- `AI_main`: AI 서버 메인 브랜치
- `main`: 전체 프로젝트 메인 브랜치

**이슈 보고**: GitHub Issues 활용

---

**작성일**: 2025-01-11
**최종 수정**: 2025-01-11
**버전**: v6.0
**작성자**: Claude Code

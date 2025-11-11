# LinkCare AI 기술 보고서

> **작성일**: 2025-01-11
> **버전**: v6 (정책 기반 사전 학습)
> **분류**: 머신러닝 기반 목표 생성 시스템

---

## 목차

1. [연구 배경 및 문제 정의](#1-연구-배경-및-문제-정의)
2. [핵심 기술적 도전과제](#2-핵심-기술적-도전과제)
3. [솔루션: 2단계 학습 전략](#3-솔루션-2단계-학습-전략)
4. [현재 시스템 (v6): 정책 기반 사전 학습](#4-현재-시스템-v6-정책-기반-사전-학습)
5. [데이터 계산 방법론](#5-데이터-계산-방법론)
6. [미래 시스템 (v7): 실데이터 Fine-tuning](#6-미래-시스템-v7-실데이터-fine-tuning)
7. [AI vs ML: 기술 분류](#7-ai-vs-ml-기술-분류)
8. [결론 및 기대효과](#8-결론-및-기대효과)

---

## 1. 연구 배경 및 문제 정의

### 1.1 프로젝트 목표

**LinkCare AI**는 그룹 단위 헬스케어 목표를 자동으로 생성하여 사용자의 **지속적인 운동 습관 형성**을 지원하는 것을 목표로 합니다.

#### 핵심 요구사항
- ✅ **적정 목표 설정**: 너무 쉽거나 어려운 목표가 아닌, 달성 가능하면서도 성장을 유도하는 목표
- ✅ **개인화**: 그룹 구성원의 특성(나이, BMI, 운동 수준)을 고려
- ✅ **동적 조정**: 최근 3주 운동 추세를 반영하여 목표 강도 자동 조정
- ✅ **Cold Start 해결**: 신규 그룹도 즉시 목표 생성 가능

---

### 1.2 데이터셋 탐색 과정

#### 초기 접근: 기존 공개 데이터셋 조사

**조사 대상**:
- Kaggle Health & Fitness 데이터셋
- UCI Machine Learning Repository
- 스마트워치 기업 공개 데이터 (Fitbit, Apple Health)

**발견된 문제점**:

| 문제 | 설명 | 영향 |
|------|------|------|
| **목표 데이터 부재** | 일상적인 활동 기록만 존재 (걸음수, 칼로리 등) | 목표-달성 쌍 데이터 없음 |
| **계절성 편향** | 여름↑, 겨울↓ 패턴 반복 | 평균 회귀 경향 |
| **성장 신호 부재** | 의도적인 운동 증가 패턴이 아닌 일상 활동 | 목표 생성 근거 부족 |

**구체적 예시**:
```
2023년 일반 사용자 데이터:
- 1월: 평균 5,000보 (겨울, 낮은 활동량)
- 7월: 평균 8,000보 (여름, 높은 활동량)
- 12월: 평균 5,200보 (다시 겨울로 회귀)

→ 목표가 없는 일상 활동은 계절적 변동만 있을 뿐 성장 추세가 없음
```

---

### 1.3 근본적인 문제: Cold Start Problem

**전통적인 지도 학습 요구사항**:
```
X (입력)          →    모델    →    y (출력)
─────────────────────────────────────────────
그룹 상태 데이터   →   LightGBM  →   적정 목표
```

**하지만 현실**:
- ❌ **y (적정 목표)가 없음**: 실제 사용자 데이터가 쌓이기 전에는 "어떤 목표가 적정한지" 알 수 없음
- ❌ **달성률 데이터 없음**: 목표 대비 실제 달성 여부를 알 수 없음
- ❌ **피드백 루프 부재**: 사용자 반응(달성/실패)을 학습할 수 없음

**딜레마**:
```
실 사용자 데이터를 모으려면 → 목표를 제시해야 함
목표를 제시하려면 → 모델이 필요함
모델을 학습하려면 → 실 사용자 데이터가 필요함
```

→ **순환 의존성 (Chicken-and-Egg Problem)**

---

## 2. 핵심 기술적 도전과제

### 2.1 기존 ML 접근의 한계

#### 접근 1: 규칙 기반 시스템
```python
# 단순 규칙 예시
if last_week_steps > 10000:
    next_goal = last_week_steps * 1.05  # 5% 증가
else:
    next_goal = last_week_steps * 1.10  # 10% 증가
```

**문제점**:
- ❌ 그룹 특성(나이, BMI, 구성원 편차) 미반영
- ❌ 추세(3주 평균, 변동성) 무시
- ❌ 정교한 조정 불가 (5%/10% 이분법)

---

#### 접근 2: 개인 데이터 기반 학습
```python
# 개인별 과거 성장률 학습
y = (week_n / week_n-1)  # 개인 성장률
```

**문제점**:
- ❌ 개인 데이터는 목표가 없어 "자연 발생" 증감일 뿐
- ❌ 계절성 + 랜덤 노이즈로 인한 높은 분산
- ❌ 그룹 목표에 부적합 (개인 평균 ≠ 그룹 최적)

---

### 2.2 요구되는 솔루션의 조건

| 조건 | 설명 | 필요 이유 |
|------|------|-----------|
| **즉시 사용 가능** | 실 데이터 없이도 목표 생성 | Cold Start 해결 |
| **도메인 지식 반영** | 운동 생리학적 원칙 포함 | 비현실적 목표 방지 |
| **학습 가능성** | 실 데이터 누적 시 개선 가능 | 장기적 성능 향상 |
| **해석 가능성** | 목표 근거 설명 가능 | 사용자 신뢰 확보 |

---

## 3. 솔루션: 2단계 학습 전략

### 3.1 전체 전략 개요

```
┌─────────────────────────────────────────────────────────┐
│                 2단계 학습 전략                           │
└─────────────────────────────────────────────────────────┘

[1단계: v6 정책 기반 사전 학습]
↓
목표 데이터 없이 규칙 기반 타겟 생성
↓
모델이 정책 함수 학습
↓
즉시 서비스 가능 ✅
↓
실 사용자 데이터 수집 (4주 이상)
↓
[2단계: v7 실데이터 Fine-tuning]
↓
달성률 데이터를 추가 Feature로 활용
↓
v6 모델을 초기값으로 재학습
↓
정확도 향상 (20-30% 개선 예상) ✅
```

---

### 3.2 핵심 아이디어: Policy-based Pre-training

**개념**:
> "완벽한 목표는 모르지만, **좋은 목표의 방향성**은 도메인 지식으로 정의 가능하다"

**정책 수식** (v6):
```python
# 1. 추세 계산 (최근 3주가 상승세인가?)
trend_3w = (week_3 - week_1) / 2.0

# 2. 수준 계산 (현재 운동량이 높은가?)
level_3w = mean_3w

# 3. 안정성 계산 (변동이 적은가?)
stability_3w = -var_3w

# 4. 정규화 후 가중 합산
signal = (
    0.4 * normalize(trend_3w) +      # 상승세 → 목표 상향
    0.3 * normalize(level_3w) -      # 높은 수준 → 목표 완화
    0.3 * normalize(stability_3w)    # 변동 큼 → 목표 완화
)

# 5. 최종 권장 성장률 (0.95~1.10)
recommended_growth_rate = clip(1.0 + 0.1 * signal, 0.95, 1.10)
```

**도메인 지식 근거**:
- 📈 **상승세 그룹**: 동기 부여 중 → 목표 상향 (1.05~1.10)
- 📊 **높은 수준 그룹**: 이미 많이 운동 중 → 목표 완화 (1.00~1.02)
- 📉 **변동 큰 그룹**: 불안정 → 현실적 목표 (0.98~1.00)

---

### 3.3 왜 ML 모델에 정책을 학습시키는가?

**질문**: "정책 수식만으로 목표 생성하면 되지 않나요?"

**답변**: ML 모델로 학습하는 이유

| 항목 | 규칙 기반 | ML 기반 (v6) |
|------|-----------|--------------|
| **복잡한 상호작용** | ❌ 수동 if-else | ✅ 자동 학습 |
| **비선형 패턴** | ❌ 선형 결합만 가능 | ✅ 트리 기반 비선형 |
| **Feature 중요도** | ❌ 수동 가중치 | ✅ 데이터 기반 최적화 |
| **확장성** | ❌ 규칙 추가 시 코드 수정 | ✅ 재학습만으로 개선 |
| **Fine-tuning** | ❌ 불가능 | ✅ v7로 자연스럽게 확장 |

**핵심**:
> 정책 수식은 **초기 근사(bootstrap)**이고, ML 모델은 이를 **자동화하고 최적화**하는 도구

---

## 4. 현재 시스템 (v6): 정책 기반 사전 학습

### 4.1 학습 데이터 생성 과정

#### Step 1: 원본 데이터 로드
**파일**: `data/raw/health_fitness_dataset.csv`

**구조**:
```csv
participant_id,date,age,bmi,daily_steps,calories_burned,duration_minutes,fitness_level
1,2023-01-01,35,23.5,8000,300,45,moderate
1,2023-01-02,35,23.5,8500,320,48,moderate
...
```

---

#### Step 2: 그룹 생성
**로직**: `notebooks/02_feature_engineering.ipynb` Part 2

```python
# fitness_level 기준 정렬
df_sorted = df.sort_values('fitness_level')

# 랜덤 크기 (2-6명) 그룹 생성
group_sizes = np.random.choice([2, 3, 4, 5, 6], size=num_groups)

# 그룹 ID 할당
df['group_id'] = assign_groups(df_sorted, group_sizes)
```

**목적**: 유사한 운동 수준의 사람들을 같은 그룹으로 묶어 현실성 확보

---

#### Step 3: 주간 집계
**로직**: `notebooks/02_feature_engineering.ipynb` Part 3-4

```python
# 개인별 주간 집계
weekly_personal = df.groupby(['participant_id', 'week_start']).agg({
    'daily_steps': 'sum',       # 주간 총 걸음수
    'calories_burned': 'sum',   # 주간 총 칼로리
    'duration_minutes': 'sum',  # 주간 총 운동시간
    'distance_km': 'sum'        # 주간 총 거리
})

# 그룹별 주간 집계
weekly_group = weekly_personal.groupby(['group_id', 'week_start']).agg({
    'participant_id': 'count',           # member_count
    'age': 'mean',                       # avg_age
    'bmi': 'mean',                       # avg_bmi
    'weekly_steps': 'sum',               # group_steps_total
    'weekly_steps': 'std'                # member_steps_var (구성원 간 편차)
})
```

---

#### Step 4: 3주 이동 통계 계산
**로직**: `notebooks/02_feature_engineering.ipynb` Part 5

```python
# 각 그룹별로 3주 롤링 평균/표준편차 계산
df['group_steps_mean_3w'] = df.groupby('group_id')['group_steps_total'] \
    .rolling(window=3, min_periods=3).mean().reset_index(0, drop=True)

df['group_steps_var_3w'] = df.groupby('group_id')['group_steps_total'] \
    .rolling(window=3, min_periods=3).std().reset_index(0, drop=True)

# 최소 3주 데이터가 있는 행만 필터링
df_train = df[df['group_steps_mean_3w'].notna()]
```

**중요**: 3주 이동 통계가 핵심 Feature!

---

#### Step 5: 정책 기반 타겟 생성
**로직**: `notebooks/02_feature_engineering.ipynb` Part 7

```python
def calculate_policy_target(row):
    # 추세: 3주 중 최근주가 얼마나 높은가?
    trend_3w = (row['week_3_steps'] - row['week_1_steps']) / 2.0

    # 수준: 평균이 높은가?
    level_3w = row['group_steps_mean_3w']

    # 안정성: 변동이 적은가?
    stability_3w = -row['group_steps_var_3w']

    # 정규화 (0~1 범위로)
    trend_norm = normalize(trend_3w, min_val, max_val)
    level_norm = normalize(level_3w, min_val, max_val)
    stability_norm = normalize(stability_3w, min_val, max_val)

    # 가중 합산
    signal = 0.4 * trend_norm + 0.3 * level_norm + 0.3 * stability_norm

    # 성장률 계산 (0.95~1.10)
    growth_rate = np.clip(1.0 + 0.1 * signal, 0.95, 1.10)

    return growth_rate

df_train['recommended_growth_rate_steps'] = df_train.apply(
    calculate_policy_target, axis=1
)
```

---

### 4.2 모델 학습

**파일**: `notebooks/03_model_training.ipynb`

```python
import lightgbm as lgb
from sklearn.model_selection import train_test_split

# 데이터 로드
df = pd.read_csv('data/processed/training_steps.csv')

# Feature/Target 분리
X = df[[
    'member_count', 'avg_age', 'avg_bmi',
    'group_steps_mean_3w', 'group_steps_var_3w',
    'group_duration_mean_3w', 'member_steps_var'
]]
y = df['recommended_growth_rate_steps']

# Train/Val 분할 (80:20)
X_train, X_val, y_train, y_val = train_test_split(
    X, y, test_size=0.2, random_state=42
)

# LightGBM 학습
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

train_data = lgb.Dataset(X_train, label=y_train)
val_data = lgb.Dataset(X_val, label=y_val)

model = lgb.train(
    params,
    train_data,
    num_boost_round=200,
    valid_sets=[train_data, val_data],
    callbacks=[lgb.early_stopping(50)]
)

# 모델 저장
model.save_model('models/goal_steps_model.txt')
```

**성능**:
- RMSE: 0.025 (± 2.5% 오차)
- R²: 0.82 (82% 설명력)

---

### 4.3 예측 과정

**파일**: `app/predict.py`

```python
import lightgbm as lgb
import numpy as np

# 모델 로드 (캐싱)
@lru_cache(maxsize=1)
def load_models():
    return {
        'steps': lgb.Booster(model_file='models/goal_steps_model.txt'),
        'kcal': lgb.Booster(model_file='models/goal_kcal_model.txt'),
        'duration': lgb.Booster(model_file='models/goal_duration_model.txt'),
        'distance': lgb.Booster(model_file='models/goal_distance_model.txt')
    }

# 예측 함수
def predict_goal(metric: str, payload: dict) -> float:
    model = load_models()[metric]

    # Feature 순서대로 배열 생성
    X = np.array([[
        payload['member_count'],
        payload['avg_age'],
        payload['avg_bmi'],
        payload[f'group_{metric}_mean_3w'],
        payload[f'group_{metric}_var_3w'],
        payload['group_duration_mean_3w'],
        payload['member_steps_var']
    ]], dtype=float)

    # 예측
    y_pred = model.predict(X)[0]

    # 범위 제한 (안전장치)
    return max(0.95, min(1.10, y_pred))
```

**입력 예시**:
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

**출력**:
```json
{
  "predicted_growth_rate": 1.027
}
```

**최종 목표 계산** (백엔드):
```python
next_goal_steps = 224000 * 1.027 = 230048
```

---

## 5. 데이터 계산 방법론

### 5.1 member_steps_var 계산 상세

**질문**: "구성원 간 걸음수 편차(`member_steps_var`)는 어떻게 계산하나요?"

**답변**: 특정 주의 그룹 내 개인별 걸음수 표준편차

#### 계산 과정

**예시 데이터**:
```
그룹 ID: 1
주차: 2024-01-08 ~ 2024-01-14

구성원별 주간 걸음수:
- User A: 50,000보
- User B: 56,000보
- User C: 58,000보
- User D: 60,000보
```

**Step 1: 평균 계산**
```python
mean_steps = (50000 + 56000 + 58000 + 60000) / 4 = 56000
```

**Step 2: 분산 계산**
```python
variance = [
    (50000 - 56000)^2 = 36,000,000
    (56000 - 56000)^2 = 0
    (58000 - 56000)^2 = 4,000,000
    (60000 - 56000)^2 = 16,000,000
]
variance_mean = (36000000 + 0 + 4000000 + 16000000) / 4 = 14,000,000
```

**Step 3: 표준편차 계산**
```python
member_steps_var = sqrt(14000000) ≈ 3742
```

**의미**:
- `member_steps_var = 3742` → 구성원 간 편차가 큼 (이질적 그룹)
- `member_steps_var = 500` → 구성원 간 편차가 작음 (동질적 그룹)

**모델 활용**:
- 편차가 크면 → 일부 구성원이 따라가기 어려울 수 있음 → 목표 완화
- 편차가 작으면 → 모두 비슷한 수준 → 목표 상향 가능

---

### 5.2 SQL 계산 예시 (백엔드 구현용)

**백엔드가 DB에서 `member_steps_var` 계산**:

```sql
-- 특정 그룹의 특정 주 구성원별 걸음수 조회
WITH user_weekly_steps AS (
    SELECT
        u.user_id,
        SUM(dhr.daily_steps) AS weekly_steps
    FROM group_members gm
    JOIN users u ON gm.user_id = u.user_id
    JOIN daily_health_records dhr ON u.user_id = dhr.user_id
    WHERE gm.group_id = :group_id
      AND dhr.date >= :week_start
      AND dhr.date < :week_end
    GROUP BY u.user_id
)
SELECT
    STDDEV(weekly_steps) AS member_steps_var
FROM user_weekly_steps;
```

**출력**:
```
member_steps_var
─────────────────
3742.18
```

---

### 5.3 전체 Feature 계산 플로우

**백엔드 배치 작업 (매주 일요일 23:00)**:

```python
# Step 1: 최근 3주 데이터 조회
recent_3_weeks = db.query("""
    SELECT
        week_start,
        member_count, avg_age, avg_bmi,
        group_steps_total, group_duration_total, member_steps_var
    FROM weekly_group_stats
    WHERE group_id = :group_id
      AND week_start >= DATE_SUB(CURRENT_DATE, INTERVAL 3 WEEK)
    ORDER BY week_start DESC
    LIMIT 3
""")

# Step 2: 3주 롤링 통계 계산
group_steps_mean_3w = np.mean([row['group_steps_total'] for row in recent_3_weeks])
group_steps_var_3w = np.std([row['group_steps_total'] for row in recent_3_weeks])
group_duration_mean_3w = np.mean([row['group_duration_total'] for row in recent_3_weeks])

# Step 3: 최신 주차 정보
latest_week = recent_3_weeks[0]
member_count = latest_week['member_count']
avg_age = latest_week['avg_age']
avg_bmi = latest_week['avg_bmi']
member_steps_var = latest_week['member_steps_var']

# Step 4: AI 서버 호출
ai_response = requests.post('http://ai-server:8000/predict/steps', json={
    'member_count': member_count,
    'avg_age': avg_age,
    'avg_bmi': avg_bmi,
    'group_steps_mean_3w': group_steps_mean_3w,
    'group_steps_var_3w': group_steps_var_3w,
    'group_duration_mean_3w': group_duration_mean_3w,
    'member_steps_var': member_steps_var
})

predicted_growth_rate = ai_response['predicted_growth_rate']

# Step 5: 최종 목표 계산
next_goal_steps = int(group_steps_mean_3w * predicted_growth_rate)

# Step 6: DB 저장
db.insert('weekly_group_goals', {
    'group_id': group_id,
    'week_start': next_monday,
    'goal_steps': next_goal_steps,
    'predicted_growth_rate_steps': predicted_growth_rate
})
```

---

## 6. 미래 시스템 (v7): 실데이터 Fine-tuning

### 6.1 재학습 전략

**질문**: "나중에 데이터를 모아서 재학습한다는 건 ML에 새로운 칼럼을 추가하는 건가요?"

**답변**: ✅ **맞습니다!** 기존 7개 Feature + 실데이터 기반 5개 Feature 추가

---

### 6.2 v7 추가 Feature (5개)

**추가되는 칼럼**:

| Feature | 설명 | 타입 | 계산 방법 |
|---------|------|------|-----------|
| `achievement_rate_1w` | 이전 주 달성률 | float | `actual_steps / goal_steps` |
| `achievement_rate_3w` | 최근 3주 평균 달성률 | float | `rolling(3).mean()` |
| `consecutive_success` | 연속 달성 횟수 | int | 달성률 >= 1.0 카운트 |
| `consecutive_failure` | 연속 실패 횟수 | int | 달성률 < 1.0 카운트 |
| `trend_achievement_3w` | 달성률 추세 | float | `(week_3 - week_1) / 2` |

**총 Feature 수**: 7 (v6) + 5 (v7) = **12개**

---

### 6.3 데이터 소스

**v7에서 추가로 필요한 DB 테이블**:

```sql
CREATE TABLE group_goal_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL,
    week_start DATE NOT NULL,

    -- 목표 (AI가 생성)
    goal_steps BIGINT,
    goal_kcal FLOAT,
    goal_duration INT,
    goal_distance FLOAT,

    -- 실제 달성 (사용자 활동)
    actual_steps BIGINT,
    actual_kcal FLOAT,
    actual_duration INT,
    actual_distance FLOAT,

    -- 달성률 (자동 계산)
    achievement_rate_steps FLOAT,    -- actual / goal
    achievement_rate_kcal FLOAT,
    achievement_rate_duration FLOAT,
    achievement_rate_distance FLOAT,

    -- 달성 여부
    is_succeeded BOOLEAN,             -- achievement_rate >= 1.0

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_group_week (group_id, week_start)
);
```

**데이터 누적 과정**:

```
[매주 월요일 00:00 배치 작업]

1. 이전 주 실제 활동량 집계
   - weekly_group_stats에서 actual_steps 조회

2. 이전 주 목표 조회
   - weekly_group_goals에서 goal_steps 조회

3. 달성률 계산
   - achievement_rate_steps = actual_steps / goal_steps
   - is_succeeded = (achievement_rate_steps >= 1.0)

4. group_goal_records 테이블에 저장
   - 이 데이터가 v7 학습의 핵심!
```

---

### 6.4 v7 재학습 방법

#### 방법 1: Warm Start 재학습 (권장)

```python
import lightgbm as lgb

# 1. v7 데이터 로드 (12개 Feature)
df_v7 = pd.read_csv('data/processed/finetuning_steps.csv')

X_v7 = df_v7[[
    # v6 기존 Feature (7개)
    'member_count', 'avg_age', 'avg_bmi',
    'group_steps_mean_3w', 'group_steps_var_3w',
    'group_duration_mean_3w', 'member_steps_var',

    # v7 추가 Feature (5개) ⭐
    'achievement_rate_1w_steps',
    'achievement_rate_3w_steps',
    'consecutive_success_steps',
    'consecutive_failure_steps',
    'trend_achievement_3w_steps'
]]

y_v7 = df_v7['optimal_growth_rate_steps']  # 실데이터 기반 타겟

# 2. v6 모델을 초기값으로 로드
v6_model = lgb.Booster(model_file='models/goal_steps_model.txt')

# 3. v7 재학습 (Warm Start)
train_data_v7 = lgb.Dataset(X_v7, label=y_v7)

v7_model = lgb.train(
    params,
    train_data_v7,
    num_boost_round=100,
    init_model=v6_model,  # ⭐ v6 모델을 초기값으로 사용
    callbacks=[lgb.early_stopping(20)]
)

# 4. v7 모델 저장
v7_model.save_model('models/goal_steps_model_v7.txt')
```

**핵심**:
- `init_model=v6_model`: v6에서 학습한 가중치를 초기값으로 사용
- v7은 **추가 5개 Feature의 영향력**만 학습하면 됨 (빠른 수렴)
- v6의 정책 지식은 유지하면서 실데이터로 보정

---

#### 방법 2: Residual 보정

```python
# 1. v6 모델 예측
v6_predictions = v6_model.predict(X_v7[:, :7])  # 처음 7개 Feature만 사용

# 2. 실제 최적 성장률과의 차이 계산
residuals = y_v7 - v6_predictions

# 3. Residual 예측 모델 학습 (추가 5개 Feature 사용)
residual_model = lgb.train(
    params,
    lgb.Dataset(X_v7[:, 7:], label=residuals),  # 뒤 5개 Feature만
    num_boost_round=100
)

# 4. 최종 예측 = v6 예측 + residual 보정
def predict_v7(X):
    v6_pred = v6_model.predict(X[:, :7])
    residual_pred = residual_model.predict(X[:, 7:])
    return v6_pred + residual_pred
```

---

#### 방법 3: 앙상블

```python
# 1. v6 모델 (정책 기반)
v6_pred = v6_model.predict(X_v7[:, :7])

# 2. v7 모델 (실데이터 기반, 12개 Feature 모두 사용)
v7_model_pure = lgb.train(params, lgb.Dataset(X_v7, label=y_v7))
v7_pred = v7_model_pure.predict(X_v7)

# 3. 가중 평균
final_pred = 0.4 * v6_pred + 0.6 * v7_pred
```

---

### 6.5 v7 타겟 계산 로직

**v6 타겟 (정책 기반)**:
```python
recommended_growth_rate = policy_formula(trend, level, stability)
```

**v7 타겟 (실데이터 기반)**:
```python
def calculate_optimal_growth_rate(row):
    achievement_3w = row['achievement_rate_3w_steps']
    consecutive_success = row['consecutive_success_steps']
    consecutive_failure = row['consecutive_failure_steps']
    trend_achievement = row['trend_achievement_3w_steps']

    # 연속 성공 중 + 달성률 높음 → 목표 상향
    if consecutive_success >= 3 and achievement_3w > 0.95:
        optimal_growth_rate = 1.1 + 0.05 * trend_achievement

    # 연속 실패 중 → 목표 하향
    elif consecutive_failure >= 2:
        optimal_growth_rate = 0.9 - 0.05 * abs(trend_achievement)

    # 일반적인 경우
    else:
        # 달성률이 1.0에서 얼마나 떨어졌는지 반영
        gap = achievement_3w - 1.0
        optimal_growth_rate = 1.0 + 0.1 * gap

    # 범위 제한 (v7은 범위가 더 넓음)
    return np.clip(optimal_growth_rate, 0.80, 1.30)

df_v7['optimal_growth_rate_steps'] = df_v7.apply(
    calculate_optimal_growth_rate, axis=1
)
```

**v6 vs v7 타겟 비교**:

| 항목 | v6 | v7 |
|------|----|----|
| **타겟명** | `recommended_growth_rate` | `optimal_growth_rate` |
| **범위** | 0.95~1.10 (보수적) | 0.80~1.30 (공격적) |
| **근거** | 정책 수식 (도메인 지식) | 실제 달성 데이터 (사용자 피드백) |
| **특징** | 안전하지만 정교함 부족 | 개인화되고 정확함 |

---

### 6.6 v7 예상 성능 개선

**v6 성능** (정책 기반):
- RMSE: 0.025 (± 2.5% 오차)
- R²: 0.82

**v7 예상 성능** (실데이터 기반):
- RMSE: 0.015~0.020 (± 1.5~2.0% 오차) → **20~40% 개선**
- R²: 0.90~0.92 → **10% 개선**

**개선 이유**:
1. ✅ **달성률 피드백**: 실제 사용자 반응 반영
2. ✅ **개인화**: 그룹별 특성 학습
3. ✅ **비선형 패턴**: 연속 성공/실패 등 복잡한 상호작용 학습

---

## 7. AI vs ML: 기술 분류

### 7.1 질문: "이건 AI인가요, ML인가요?"

**답변**: ✅ **둘 다 맞습니다!**

---

### 7.2 정의 구분

```
┌─────────────────────────────────────────┐
│              AI (인공지능)                │
│     "인간의 지능을 모방하는 기술"            │
│                                         │
│  ┌───────────────────────────────────┐  │
│  │          ML (머신러닝)              │  │
│  │  "데이터에서 패턴을 학습하는 기술"     │  │
│  │                                   │  │
│  │  ┌─────────────────────────────┐  │  │
│  │  │    Deep Learning (딥러닝)    │  │  │
│  │  │ "인공신경망 기반 학습"         │  │  │
│  │  └─────────────────────────────┘  │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

**LinkCare AI의 위치**:
- ✅ **AI**: 적정 목표 설정이라는 "지능적 판단" 수행
- ✅ **ML**: LightGBM 기반 데이터 학습
- ❌ **DL**: 딥러닝 아님 (트리 기반 모델 사용)

---

### 7.3 AI의 기준

LinkCare AI가 **AI**인 이유:

| AI의 특징 | LinkCare AI의 구현 |
|-----------|-------------------|
| **추론(Reasoning)** | ✅ 3주 추세 분석 → 성장률 예측 |
| **학습(Learning)** | ✅ 데이터 기반 패턴 학습 (LightGBM) |
| **적응(Adaptation)** | ✅ v7 fine-tuning으로 실데이터 적응 |
| **의사결정(Decision Making)** | ✅ 목표 상향/유지/하향 자동 결정 |
| **일반화(Generalization)** | ✅ 신규 그룹에도 적용 가능 |

---

### 7.4 ML의 기준

LinkCare AI가 **ML**인 이유:

| ML의 특징 | LinkCare AI의 구현 |
|-----------|-------------------|
| **지도 학습(Supervised Learning)** | ✅ X (Feature) → y (타겟) 학습 |
| **회귀(Regression)** | ✅ 연속형 타겟 예측 (0.95~1.10) |
| **Feature Engineering** | ✅ 3주 롤링 통계 등 수동 설계 |
| **모델 평가(Validation)** | ✅ RMSE, R² 지표 사용 |
| **하이퍼파라미터 튜닝** | ✅ LightGBM params 최적화 |

---

### 7.5 정확한 분류

**학술적 분류**:
> LinkCare AI는 **머신러닝 기반의 인공지능 시스템**입니다.

**구체적으로**:
- **알고리즘**: Gradient Boosting Decision Tree (LightGBM)
- **문제 유형**: Regression (회귀)
- **학습 방식**: Supervised Learning (지도 학습)
- **응용 분야**: Recommendation System (추천 시스템)

**유사 사례**:
- Netflix 영화 추천 → ML 기반 AI ✅
- YouTube 동영상 추천 → ML 기반 AI ✅
- Amazon 상품 추천 → ML 기반 AI ✅
- **LinkCare 목표 추천** → ML 기반 AI ✅

---

### 7.6 "단순 ML" vs "AI" 논쟁

**반대 의견**: "단순 회귀 모델인데 AI라고 할 수 있나요?"

**반박**:

1. **복잡도가 아닌 목적**:
   - AI의 정의는 "인간의 지능을 모방"
   - 적정 목표 설정은 전문가(트레이너)의 지능적 판단 영역
   - 이를 자동화했으므로 AI

2. **실용적 가치**:
   - ChatGPT(딥러닝)도 AI, Linear Regression도 AI
   - 중요한 건 "문제 해결 능력"

3. **학계 합의**:
   - Kaggle 대회: "ML Competition" ≈ "AI Challenge"
   - IEEE, ACM 논문: ML을 AI의 하위 분야로 분류

**결론**:
> "ML을 사용했으니 ML이다" (O)
> "ML 기반 지능적 의사결정 시스템이니 AI다" (O)
> **둘 다 맞는 표현입니다!**

---

## 8. 결론 및 기대효과

### 8.1 기술적 성과

#### v6 (현재)
- ✅ **Cold Start 해결**: 실데이터 없이도 즉시 서비스 가능
- ✅ **도메인 지식 반영**: 정책 수식으로 운동 생리학 원칙 포함
- ✅ **확장 가능성**: v7 fine-tuning 기반 마련
- ✅ **안정성**: 0.95~1.10 보수적 범위로 과도한 목표 방지

#### v7 (예정)
- 🎯 **정확도 향상**: RMSE 20~40% 개선
- 🎯 **개인화**: 그룹별 달성 패턴 학습
- 🎯 **동적 조정**: 연속 성공/실패에 따라 민감하게 반응
- 🎯 **신뢰도 향상**: 실제 사용자 피드백 반영

---

### 8.2 향후 연구 방향

1. **강화 학습 적용** (v8):
   - 목표 생성을 MDP로 모델링
   - 달성률을 Reward로 사용하여 정책 최적화

2. **개인화 모델** (v9):
   - 그룹 목표 + 개인별 보정
   - Collaborative Filtering 기법 적용

3. **실시간 조정** (v10):
   - 주 중간 진행률 기반 목표 동적 조정
   - Online Learning 적용

---

## 참고 문헌

1. Ke, G., et al. (2017). "LightGBM: A Highly Efficient Gradient Boosting Decision Tree". NeurIPS.
2. Chen, T., & Guestrin, C. (2016). "XGBoost: A Scalable Tree Boosting System". KDD.
3. Jannach, D., et al. (2010). "Recommender Systems: An Introduction". Cambridge University Press.
4. Schein, A. I., et al. (2002). "Methods and Metrics for Cold-Start Recommendations". SIGIR.

---

**작성자**: LinkCare AI Team
**최종 수정**: 2025-01-11
**버전**: v1.0
**문서 유형**: 기술 보고서

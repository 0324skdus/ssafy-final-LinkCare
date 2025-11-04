# 02_feature_engineering.ipynb 수정 가이드

**대상**: `notebooks/02_feature_engineering.ipynb`  
**목적**: 개인별 추세 Feature를 추가하여 성장률 예측 기반 학습 데이터 생성  
**프로젝트 경로**: `C:/Users/SSAFY/Desktop/LinkCareAI`

---

## 🎯 수정 목표

기존 **절대값 예측** 방식에서 **성장률 예측** 방식으로 전환하기 위한 학습 데이터 생성

### 변경 사항 요약
1. **개인별 추세 Feature 4개 추가**
   - `avg_growth_rate_members_<metric>`: 개인별 성장률 평균
   - `var_growth_rate_members_<metric>`: 개인별 성장률 분산 ⭐
   - `avg_achievement_members`: 개인별 달성률 평균
   - `trend_achievement_members`: 개인별 달성률 추세 ⭐

2. **타겟 변경**: 절대값 → 성장률
   - `next_goal_steps` → `growth_rate_steps`

3. **Feature 제거**: `season` 제거

4. **최종 Feature 수**: 9개 → 13개 (입력 12개 + 타겟 1개)

---

## 📋 수정 Step-by-Step

### Step 0: 현재 노트북 구조 파악

현재 `02_feature_engineering.ipynb`는 아래와 같은 구조일 것으로 예상:

```python
# 1. 데이터 로드
df = pd.read_csv('../data/raw/health_fitness_dataset.csv')

# 2. 불필요한 컬럼 제거
df = df[['participant_id', 'date', 'age', 'bmi', ...]]

# 3. 그룹 생성 (fitness_level 기준, 2-6명 랜덤)
# ...

# 4. 주차별 집계
weekly_group_stats = df.groupby(['group_id', 'year', 'week']).agg({...})

# 5. 기존 Feature 생성 (9개)
# - member_count, group_avg_steps_14, last_week_achievement_rate, etc.

# 6. 타겟 생성 (절대값)
weekly_group_stats['next_goal_steps'] = ...

# 7. 학습 데이터 저장
training_steps.to_csv('../data/processed/training_steps.csv')
```

---

### Step 1: 개인별 주차 데이터 추적 추가

**위치**: 주차별 집계 직후 (Step 4 다음)

```python
# ============================================================
# Step 1: 개인별 주차 데이터 계산
# ============================================================
print("\n📊 Step 1: 개인별 주차 데이터 계산 중...")

individual_weekly = df.groupby(['participant_id', 'group_id', 'year', 'week']).agg({
    'daily_steps': 'mean',
    'calories_burned': 'mean',
    'duration_minutes': 'mean',
    'distance_km': 'mean',
    'date': 'count'  # 활동일 수
}).reset_index()

# 컬럼명 변경
individual_weekly.columns = [
    'participant_id', 'group_id', 'year', 'week',
    'ind_avg_steps', 'ind_avg_kcal', 'ind_avg_duration', 'ind_avg_distance',
    'ind_active_days'
]

# 개인별 활동일 비율
individual_weekly['ind_active_days_rate'] = (individual_weekly['ind_active_days'] / 7).round(2)

print(f"   ✅ 개인별 주차 데이터: {len(individual_weekly)} rows")
print(f"   컬럼: {list(individual_weekly.columns)}")
individual_weekly.head()
```

---

### Step 2: 개인별 성장률 계산

**위치**: Step 1 직후

```python
# ============================================================
# Step 2: 개인별 성장률 계산 (최근 3~4주)
# ============================================================
print("\n📈 Step 2: 개인별 성장률 계산 중...")

# 정렬 (시계열 순서 보장)
individual_weekly = individual_weekly.sort_values(['participant_id', 'year', 'week'])

# 각 개인의 지난 주 대비 성장률
for metric in ['steps', 'kcal', 'duration', 'distance']:
    # pct_change()는 증가율(-1~1)을 반환하므로 +1하면 비율(0~2)
    individual_weekly[f'ind_growth_rate_{metric}'] = (
        individual_weekly.groupby('participant_id')[f'ind_avg_{metric}']
        .pct_change() + 1
    )
    
    # 결측값 처리 (첫 주는 성장률 계산 불가 → 1.0으로 초기화)
    individual_weekly[f'ind_growth_rate_{metric}'].fillna(1.0, inplace=True)
    
    # 이상치 제거 (0.3 ~ 2.0 범위로 제한)
    individual_weekly[f'ind_growth_rate_{metric}'] = individual_weekly[f'ind_growth_rate_{metric}'].clip(0.3, 2.0)

print(f"   ✅ 성장률 컬럼 추가: ind_growth_rate_steps, ind_growth_rate_kcal, etc.")
print(f"   Steps 성장률 통계:")
print(individual_weekly['ind_growth_rate_steps'].describe())
individual_weekly[['participant_id', 'year', 'week', 'ind_avg_steps', 'ind_growth_rate_steps']].head(10)
```

---

### Step 3: 개인별 달성률 계산 (가상 목표 사용)

**위치**: Step 2 직후

```python
# ============================================================
# Step 3: 개인별 달성률 계산
# ============================================================
print("\n🎯 Step 3: 개인별 달성률 계산 중...")

# 주의: 실제 시스템에서는 실제 개인 목표 사용
# 학습 데이터에서는 그룹 평균을 개인 목표로 가정

for metric in ['steps', 'kcal', 'duration', 'distance']:
    # 그룹 평균 매핑
    individual_weekly = individual_weekly.merge(
        weekly_group_stats[['group_id', 'year', 'week', f'group_avg_{metric}_14']],
        on=['group_id', 'year', 'week'],
        how='left',
        suffixes=('', '_group')
    )
    
    # 개인 달성률 = 개인 실제 / 그룹 평균 (목표로 간주)
    individual_weekly[f'ind_achievement_{metric}'] = (
        individual_weekly[f'ind_avg_{metric}'] / 
        individual_weekly[f'group_avg_{metric}_14']
    ).fillna(1.0)
    
    # 이상치 제거 (0.1 ~ 3.0 범위)
    individual_weekly[f'ind_achievement_{metric}'] = individual_weekly[f'ind_achievement_{metric}'].clip(0.1, 3.0)

print(f"   ✅ 달성률 컬럼 추가: ind_achievement_steps, ind_achievement_kcal, etc.")
print(f"   Steps 달성률 통계:")
print(individual_weekly['ind_achievement_steps'].describe())
individual_weekly[['participant_id', 'year', 'week', 'ind_avg_steps', 'group_avg_steps_14', 'ind_achievement_steps']].head(10)
```

---

### Step 4: 개인별 달성률 추세 계산 (선형 회귀 기울기)

**위치**: Step 3 직후

```python
# ============================================================
# Step 4: 개인별 달성률 추세 계산 (최근 3~4주 기울기)
# ============================================================
print("\n📉 Step 4: 개인별 달성률 추세 계산 중...")

import numpy as np

def calculate_trend(series, window=4):
    """
    최근 window 주간의 선형 회귀 기울기 계산
    
    Args:
        series: 달성률 시계열 (pandas Series)
        window: 최근 몇 주를 볼 것인지 (기본 4주)
    
    Returns:
        float: 선형 회귀 기울기 (양수=개선, 음수=피로 누적)
    """
    if len(series) < 2:
        return 0.0
    
    # 최근 window개만 사용
    series = series.iloc[-window:]
    if len(series) < 2:
        return 0.0
    
    x = np.arange(len(series))
    y = series.values
    
    # 모두 같은 값이면 기울기 0
    if np.std(y) == 0:
        return 0.0
    
    # 선형 회귀 기울기 계산
    slope = np.polyfit(x, y, 1)[0]
    return slope

# 각 개인의 달성률 추세 계산 (steps 기준)
individual_weekly['ind_trend_achievement'] = (
    individual_weekly.groupby('participant_id')['ind_achievement_steps']
    .transform(lambda x: x.rolling(window=4, min_periods=2).apply(
        lambda s: calculate_trend(s, window=len(s))
    ))
).fillna(0.0)

# 이상치 제거 (-0.5 ~ 0.5 범위)
individual_weekly['ind_trend_achievement'] = individual_weekly['ind_trend_achievement'].clip(-0.5, 0.5)

print(f"   ✅ 추세 컬럼 추가: ind_trend_achievement")
print(f"   달성률 추세 통계:")
print(individual_weekly['ind_trend_achievement'].describe())
print(f"   해석:")
print(f"     - 양수: 개인들이 개선되고 있음 (달성률 상승)")
print(f"     - 음수: 개인들이 피로 누적 중 (달성률 하락) → 번아웃 신호")
individual_weekly[['participant_id', 'year', 'week', 'ind_achievement_steps', 'ind_trend_achievement']].tail(20)
```

---

### Step 5: 개인 추세 → 그룹 Feature로 집계

**위치**: Step 4 직후

```python
# ============================================================
# Step 5: 개인 추세 → 그룹 Feature로 집계
# ============================================================
print("\n🔄 Step 5: 개인 추세를 그룹 Feature로 집계 중...")

member_trends = individual_weekly.groupby(['group_id', 'year', 'week']).agg({
    # 성장률 통계 (평균, 분산)
    'ind_growth_rate_steps': ['mean', 'var'],
    'ind_growth_rate_kcal': ['mean', 'var'],
    'ind_growth_rate_duration': ['mean', 'var'],
    'ind_growth_rate_distance': ['mean', 'var'],
    
    # 달성률 통계 (평균)
    'ind_achievement_steps': 'mean',
    
    # 달성률 추세 (평균)
    'ind_trend_achievement': 'mean',
    
    # 활동률 통계 (평균)
    'ind_active_days_rate': 'mean'
}).reset_index()

# 컬럼명 정리 (MultiIndex 제거)
member_trends.columns = [
    'group_id', 'year', 'week',
    'avg_growth_rate_members_steps', 'var_growth_rate_members_steps',
    'avg_growth_rate_members_kcal', 'var_growth_rate_members_kcal',
    'avg_growth_rate_members_duration', 'var_growth_rate_members_duration',
    'avg_growth_rate_members_distance', 'var_growth_rate_members_distance',
    'avg_achievement_members',
    'trend_achievement_members',
    'avg_active_days_rate_members'
]

print(f"   ✅ 그룹별 개인 추세 집계: {len(member_trends)} rows")
print(f"   새로운 컬럼:")
for col in member_trends.columns[3:]:  # group_id, year, week 제외
    print(f"     - {col}")

# 그룹 통계와 병합
print("\n   병합 전 weekly_group_stats 크기:", weekly_group_stats.shape)
weekly_group_stats = weekly_group_stats.merge(
    member_trends,
    on=['group_id', 'year', 'week'],
    how='left'
)
print("   병합 후 weekly_group_stats 크기:", weekly_group_stats.shape)

# 결측값 처리 (첫 주 등)
print("\n   결측값 처리 중...")
weekly_group_stats['avg_growth_rate_members_steps'].fillna(1.0, inplace=True)
weekly_group_stats['var_growth_rate_members_steps'].fillna(0.01, inplace=True)
weekly_group_stats['avg_growth_rate_members_kcal'].fillna(1.0, inplace=True)
weekly_group_stats['var_growth_rate_members_kcal'].fillna(0.01, inplace=True)
weekly_group_stats['avg_growth_rate_members_duration'].fillna(1.0, inplace=True)
weekly_group_stats['var_growth_rate_members_duration'].fillna(0.01, inplace=True)
weekly_group_stats['avg_growth_rate_members_distance'].fillna(1.0, inplace=True)
weekly_group_stats['var_growth_rate_members_distance'].fillna(0.01, inplace=True)
weekly_group_stats['avg_achievement_members'].fillna(1.0, inplace=True)
weekly_group_stats['trend_achievement_members'].fillna(0.0, inplace=True)
weekly_group_stats['avg_active_days_rate_members'].fillna(0.5, inplace=True)

print("   ✅ 결측값 처리 완료")

# 통계 확인
print("\n   📊 개인 추세 Feature 통계:")
print(weekly_group_stats[['avg_growth_rate_members_steps', 'var_growth_rate_members_steps', 
                           'avg_achievement_members', 'trend_achievement_members']].describe())

weekly_group_stats.head()
```

---

### Step 6: 성장률 타겟 생성 (절대값 대신)

**위치**: 기존 타겟 생성 코드를 대체

```python
# ============================================================
# Step 6: 성장률 타겟 생성 (절대값 → 성장률)
# ============================================================
print("\n🎯 Step 6: 성장률 타겟 계산 중...")

# ❌ 기존 코드 제거 (주석 처리)
# for metric in ['steps', 'kcal', 'duration', 'distance']:
#     weekly_group_stats[f'next_goal_{metric}'] = (
#         weekly_group_stats[f'next_week_{metric}'] * 
#         weekly_group_stats['member_count']
#     )

# ✅ 새로운 코드: 성장률 계산
for metric in ['steps', 'kcal', 'duration', 'distance']:
    # 성장률 = 다음 주 평균 / 이번 주 평균
    weekly_group_stats[f'growth_rate_{metric}'] = (
        weekly_group_stats.groupby('group_id')[f'group_avg_{metric}_14'].shift(-1) /
        weekly_group_stats[f'group_avg_{metric}_14']
    )
    
    # 이상치 제거 (0.5 ~ 1.5 범위로 제한)
    # 0.5 = -50% (급격한 감소), 1.5 = +50% (급격한 증가)
    weekly_group_stats[f'growth_rate_{metric}'] = weekly_group_stats[f'growth_rate_{metric}'].clip(0.5, 1.5)

# 성장률 통계 출력
print("\n   📊 성장률 통계:")
for metric in ['steps', 'kcal', 'duration', 'distance']:
    mean_growth = weekly_group_stats[f'growth_rate_{metric}'].mean()
    median_growth = weekly_group_stats[f'growth_rate_{metric}'].median()
    std_growth = weekly_group_stats[f'growth_rate_{metric}'].std()
    print(f"   {metric.capitalize():12} - 평균: {mean_growth:.3f}, 중앙값: {median_growth:.3f}, 표준편차: {std_growth:.3f}")

print("\n   ✅ 성장률 타겟 생성 완료")
print(f"   해석: 1.0 = 유지, 1.1 = 10% 성장, 0.9 = 10% 감소")

weekly_group_stats[['group_id', 'year', 'week', 'group_avg_steps_14', 'growth_rate_steps']].head(20)
```

---

### Step 7: season 제거 및 Feature 목록 업데이트

**위치**: 학습 데이터 저장 직전 (기존 Feature 선택 코드 수정)

```python
# ============================================================
# Step 7: Feature 목록 업데이트 (season 제거, 개인 추세 추가)
# ============================================================
print("\n📋 Step 7: 학습 데이터 Feature 선택 중...")

# ✅ Steps 학습 데이터 (13개: 입력 12개 + 타겟 1개)
steps_cols = [
    # 그룹 기본 정보 (6개)
    'member_count', 
    'group_avg_steps_14', 
    'avg_age', 
    'avg_bmi', 
    'avg_resting_hr',
    'group_weekly_active_rate',
    
    # 성과 피드백 (1개)
    'last_week_achievement_rate',
    
    # 개인 추세 (4개) ⭐ 핵심 추가 Feature
    'avg_growth_rate_members_steps',
    'var_growth_rate_members_steps',
    'avg_achievement_members',
    'trend_achievement_members',
    
    # 사용자 최소 기준 (1개)
    'user_min_steps',
    
    # 타겟 (1개) - 성장률
    'growth_rate_steps'
]

# ❌ season 제거됨
# ❌ next_goal_steps 제거됨 (growth_rate_steps로 대체)

training_steps = weekly_group_stats[steps_cols].dropna()

print(f"\n   ✅ Steps 학습 데이터 생성 완료")
print(f"   - 총 Feature 수: {len(steps_cols) - 1}개 (입력) + 1개 (타겟)")
print(f"   - 데이터 행 수: {len(training_steps)}")
print(f"   - 결측값 제거 전: {len(weekly_group_stats)}, 제거 후: {len(training_steps)}")

# Feature 목록 출력
print("\n   📊 입력 Feature (12개):")
for i, col in enumerate(steps_cols[:-1], 1):
    print(f"      {i:2}. {col}")
print(f"\n   🎯 타겟 Feature (1개):")
print(f"       {steps_cols[-1]}")

training_steps.head()
```

```python
# ✅ Kcal 학습 데이터
kcal_cols = [
    'member_count', 
    'group_avg_kcal_14', 
    'avg_age', 
    'avg_bmi', 
    'avg_resting_hr',
    'group_weekly_active_rate',
    'last_week_achievement_rate',
    'avg_growth_rate_members_kcal',
    'var_growth_rate_members_kcal',
    'avg_achievement_members',
    'trend_achievement_members',
    'user_min_kcal',
    'growth_rate_kcal'
]

training_kcal = weekly_group_stats[kcal_cols].dropna()
print(f"\n   ✅ Kcal 학습 데이터: {len(training_kcal)} rows")

# ✅ Duration 학습 데이터
duration_cols = [
    'member_count', 
    'group_avg_duration_14', 
    'avg_age', 
    'avg_bmi', 
    'avg_resting_hr',
    'group_weekly_active_rate',
    'last_week_achievement_rate',
    'avg_growth_rate_members_duration',
    'var_growth_rate_members_duration',
    'avg_achievement_members',
    'trend_achievement_members',
    'user_min_duration',
    'growth_rate_duration'
]

training_duration = weekly_group_stats[duration_cols].dropna()
print(f"   ✅ Duration 학습 데이터: {len(training_duration)} rows")

# ✅ Distance 학습 데이터
distance_cols = [
    'member_count', 
    'group_avg_distance_14', 
    'avg_age', 
    'avg_bmi', 
    'avg_resting_hr',
    'group_weekly_active_rate',
    'last_week_achievement_rate',
    'avg_growth_rate_members_distance',
    'var_growth_rate_members_distance',
    'avg_achievement_members',
    'trend_achievement_members',
    'user_min_distance',
    'growth_rate_distance'
]

training_distance = weekly_group_stats[distance_cols].dropna()
print(f"   ✅ Distance 학습 데이터: {len(training_distance)} rows")
```

---

### Step 8: user_min 계산은 유지 (변경 없음)

**위치**: 기존 코드 확인 (수정 불필요)

```python
# ============================================================
# Step 8: user_min 계산 확인 (변경 없음)
# ============================================================
print("\n✅ Step 8: user_min 계산은 기존대로 유지")
print("   - user_min_steps = group_avg_steps_14 × 0.6")
print("   - user_min_kcal = group_avg_kcal_14 × 0.6")
print("   - user_min_duration = group_avg_duration_14 × 0.6")
print("   - user_min_distance = group_avg_distance_14 × 0.6")

# 기존 코드가 있는지 확인
if 'user_min_steps' in weekly_group_stats.columns:
    print("\n   ✅ user_min 컬럼이 이미 존재합니다")
    print(weekly_group_stats[['group_avg_steps_14', 'user_min_steps']].head())
else:
    print("\n   ⚠️ user_min 컬럼이 없습니다. 생성이 필요합니다.")
    # 생성 코드 (필요 시)
    # weekly_group_stats['user_min_steps'] = (weekly_group_stats['group_avg_steps_14'] * 0.6).round()
    # ...
```

---

### Step 9: 학습 데이터 저장

**위치**: 마지막 단계

```python
# ============================================================
# Step 9: 학습 데이터 저장
# ============================================================
print("\n💾 Step 9: 학습 데이터 저장 중...")

import os

# 디렉토리 생성 (없으면)
output_dir = '../data/processed'
os.makedirs(output_dir, exist_ok=True)

# 4개 파일 저장
training_steps.to_csv(f'{output_dir}/training_steps.csv', index=False)
print(f"   ✅ training_steps.csv 저장 완료 ({len(training_steps)} rows, {len(training_steps.columns)} cols)")

training_kcal.to_csv(f'{output_dir}/training_kcal.csv', index=False)
print(f"   ✅ training_kcal.csv 저장 완료 ({len(training_kcal)} rows, {len(training_kcal.columns)} cols)")

training_duration.to_csv(f'{output_dir}/training_duration.csv', index=False)
print(f"   ✅ training_duration.csv 저장 완료 ({len(training_duration)} rows, {len(training_duration.columns)} cols)")

training_distance.to_csv(f'{output_dir}/training_distance.csv', index=False)
print(f"   ✅ training_distance.csv 저장 완료 ({len(training_distance)} rows, {len(training_distance.columns)} cols)")

print("\n🎉 모든 학습 데이터 생성 완료!")
print(f"   저장 경로: {output_dir}/")
print(f"   생성된 파일: training_steps.csv, training_kcal.csv, training_duration.csv, training_distance.csv")
```

---

### Step 10: 데이터 검증 및 시각화

**위치**: 저장 후 (선택 사항, 추천)

```python
# ============================================================
# Step 10: 데이터 검증 및 시각화 (선택)
# ============================================================
print("\n📊 Step 10: 데이터 검증 중...")

# 10-1. Feature 분포 확인
import matplotlib.pyplot as plt
import seaborn as sns

fig, axes = plt.subplots(2, 2, figsize=(15, 10))

# 개인별 성장률 평균 분포
axes[0, 0].hist(training_steps['avg_growth_rate_members_steps'], bins=50, edgecolor='black')
axes[0, 0].set_title('Distribution of avg_growth_rate_members_steps')
axes[0, 0].set_xlabel('Growth Rate')
axes[0, 0].axvline(1.0, color='red', linestyle='--', label='No Growth')
axes[0, 0].legend()

# 개인별 성장률 분산 분포
axes[0, 1].hist(training_steps['var_growth_rate_members_steps'], bins=50, edgecolor='black')
axes[0, 1].set_title('Distribution of var_growth_rate_members_steps')
axes[0, 1].set_xlabel('Variance')

# 개인별 달성률 평균 분포
axes[1, 0].hist(training_steps['avg_achievement_members'], bins=50, edgecolor='black')
axes[1, 0].set_title('Distribution of avg_achievement_members')
axes[1, 0].set_xlabel('Achievement Rate')
axes[1, 0].axvline(1.0, color='red', linestyle='--', label='100% Achievement')
axes[1, 0].legend()

# 개인별 달성률 추세 분포
axes[1, 1].hist(training_steps['trend_achievement_members'], bins=50, edgecolor='black')
axes[1, 1].set_title('Distribution of trend_achievement_members')
axes[1, 1].set_xlabel('Trend (Slope)')
axes[1, 1].axvline(0.0, color='red', linestyle='--', label='No Trend')
axes[1, 1].legend()

plt.tight_layout()
plt.savefig('../data/processed/feature_distributions.png', dpi=150)
plt.show()

print("   ✅ Feature 분포 시각화 완료 (../data/processed/feature_distributions.png)")

# 10-2. 타겟 분포 확인
plt.figure(figsize=(10, 5))
plt.hist(training_steps['growth_rate_steps'], bins=50, edgecolor='black')
plt.title('Distribution of Target: growth_rate_steps')
plt.xlabel('Growth Rate')
plt.axvline(1.0, color='red', linestyle='--', label='No Growth (1.0)')
plt.legend()
plt.savefig('../data/processed/target_distribution.png', dpi=150)
plt.show()

print("   ✅ 타겟 분포 시각화 완료 (../data/processed/target_distribution.png)")

# 10-3. 상관관계 확인
corr_cols = [
    'avg_growth_rate_members_steps',
    'var_growth_rate_members_steps',
    'avg_achievement_members',
    'trend_achievement_members',
    'growth_rate_steps'  # 타겟
]

correlation_matrix = training_steps[corr_cols].corr()

plt.figure(figsize=(10, 8))
sns.heatmap(correlation_matrix, annot=True, fmt='.3f', cmap='coolwarm', center=0)
plt.title('Correlation Matrix: Individual Trends vs Target')
plt.tight_layout()
plt.savefig('../data/processed/correlation_matrix.png', dpi=150)
plt.show()

print("   ✅ 상관관계 분석 완료 (../data/processed/correlation_matrix.png)")

# 10-4. 통계 요약
print("\n📋 최종 Feature 통계 요약:")
print(training_steps[corr_cols].describe())

print("\n✅ 데이터 검증 완료!")
```

---

## 🎯 수정 후 기대 결과

### 1. CSV 파일 (4개)
- `../data/processed/training_steps.csv` (13 columns)
- `../data/processed/training_kcal.csv` (13 columns)
- `../data/processed/training_duration.csv` (13 columns)
- `../data/processed/training_distance.csv` (13 columns)

### 2. 컬럼 구성 (Steps 기준)
```
입력 Feature (12개):
1. member_count
2. group_avg_steps_14
3. avg_age
4. avg_bmi
5. avg_resting_hr
6. group_weekly_active_rate
7. last_week_achievement_rate
8. avg_growth_rate_members_steps     ← NEW
9. var_growth_rate_members_steps     ← NEW ⭐
10. avg_achievement_members          ← NEW
11. trend_achievement_members        ← NEW ⭐
12. user_min_steps

타겟 (1개):
13. growth_rate_steps                ← CHANGED (기존: next_goal_steps)
```

### 3. 제거된 Feature
- ~~`season`~~ (계절 제거)
- ~~`next_goal_steps`~~ (절대값 → 성장률로 변경)

---

## 🚨 주의사항

### 1. 데이터 부족 시 처리
- 개인 추세 Feature는 최소 2주 이상의 데이터가 필요
- 첫 주 데이터는 기본값으로 초기화 (Step 5에서 처리)

### 2. 이상치 처리
- 성장률: 0.3 ~ 2.0 범위 (Step 2)
- 달성률: 0.1 ~ 3.0 범위 (Step 3)
- 타겟 성장률: 0.5 ~ 1.5 범위 (Step 6)

### 3. 결측값 처리
- `fillna()` 사용 (Step 5)
- 학습 데이터 저장 시 `dropna()` 사용 (Step 7)

---

## 💡 다음 단계 (노트북 수정 후)

1. **노트북 실행 및 검증**
   - 각 Step별로 실행하면서 중간 결과 확인
   - 시각화를 통한 Feature 분포 확인
   - 상관관계 분석

2. **데이터 품질 확인**
   - CSV 파일 크기 확인 (행 수가 충분한지)
   - Feature 분포가 정상적인지 확인
   - 타겟 분포가 편향되지 않았는지 확인

3. **scripts/preprocess_data.py 업데이트**
   - 검증된 코드를 스크립트로 이식
   - 자동화 파이프라인 구축

4. **모델 학습 (03_model_training.ipynb)**
   - 새로운 Feature로 모델 재학습
   - 성능 비교 (기존 vs 개선)

---

**이 가이드를 Claude Code에게 전달하면, 노트북을 단계별로 수정할 수 있습니다.**  
**각 Step은 독립적으로 실행 가능하며, 중간 결과를 확인하면서 진행할 수 있습니다.**
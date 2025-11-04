"""
원본 데이터 → 학습용 데이터 변환

🔑 핵심 개념:
- 공식으로 목표를 "계산"하지 않음
- 실제 데이터에서 다음 주에 달성한 값을 타겟으로 사용
- 모델이 실제 역사적 개선 패턴을 학습
"""
import pandas as pd
import numpy as np
from pathlib import Path
from config import RAW_DATA_DIR, PROCESSED_DATA_DIR

def get_season(month):
    """월 → 계절 변환 (1~4)"""
    if month in [3, 4, 5]:
        return 1  # 봄
    elif month in [6, 7, 8]:
        return 2  # 여름
    elif month in [9, 10, 11]:
        return 3  # 가을
    else:
        return 4  # 겨울

def remove_unused_columns(df):
    """불필요한 컬럼 제거"""
    columns_to_keep = [
        'participant_id', 'date', 'age', 'bmi', 'resting_heart_rate',
        'daily_steps', 'calories_burned', 'duration_minutes', 'fitness_level'
    ]
    return df[columns_to_keep].copy()

def create_groups_by_fitness_level(participants_df, min_size=2, max_size=6, seed=42):
    """fitness_level 기준으로 2-6명 랜덤 그룹 생성"""
    np.random.seed(seed)

    participants_sorted = participants_df.sort_values('avg_fitness_level')
    participant_ids = participants_sorted['participant_id'].values

    group_mapping = {}
    group_id = 1
    i = 0

    while i < len(participant_ids):
        remaining = len(participant_ids) - i

        if remaining < min_size and i > 0:
            group_id -= 1

        if remaining <= max_size:
            group_size = remaining
        else:
            max_possible = min(max_size, remaining - min_size)
            group_size = np.random.randint(min_size, max_possible + 1)

        for j in range(i, min(i + group_size, len(participant_ids))):
            group_mapping[participant_ids[j]] = group_id

        i += group_size
        group_id += 1

    return group_mapping

def calculate_distance_km(steps):
    """걸음수 → 거리(km) 변환"""
    return steps * 0.8 / 1000

def create_training_data():
    """
    health_fitness_dataset.csv 읽어서
    → training_steps.csv, training_kcal.csv, training_duration.csv, training_distance.csv 생성

    🔑 실제 역사적 개선률을 타겟으로 사용
    """
    print("📖 원본 데이터 로드 중...")
    df = pd.read_csv(RAW_DATA_DIR / "health_fitness_dataset.csv")
    print(f"   원본 데이터 크기: {df.shape}")

    # 1. 불필요한 컬럼 제거
    print("\n🗑️  불필요한 컬럼 제거 중...")
    df = remove_unused_columns(df)
    print(f"   정제 후 크기: {df.shape}")

    # 2. 날짜 처리
    print("\n📅 날짜 처리 중...")
    df['date'] = pd.to_datetime(df['date'])
    df['year'] = df['date'].dt.year
    df['week'] = df['date'].dt.isocalendar().week
    df['month'] = df['date'].dt.month
    df['season'] = df['month'].apply(get_season)

    # 3. 거리 계산
    print("\n🏃 거리 계산 중...")
    df['distance_km'] = calculate_distance_km(df['daily_steps'])

    # 4. 참가자별 fitness_level 계산
    print("\n👥 참가자별 fitness level 계산 중...")
    participant_fitness = df.groupby('participant_id').agg({
        'fitness_level': 'mean'
    }).reset_index()
    participant_fitness.columns = ['participant_id', 'avg_fitness_level']

    # 5. 그룹 생성
    print("\n🔀 그룹 생성 중 (2-6명, fitness level 기준)...")
    group_mapping = create_groups_by_fitness_level(participant_fitness)
    df['group_id'] = df['participant_id'].map(group_mapping)

    group_sizes = df.groupby('group_id')['participant_id'].nunique()
    print(f"   생성된 그룹 수: {len(group_sizes)}")
    print(f"   그룹 크기 분포:\n{group_sizes.value_counts().sort_index()}")

    # 6. 주차별 그룹 집계
    print("\n📊 주차별 그룹 데이터 집계 중...")
    weekly_group_stats = df.groupby(['group_id', 'year', 'week']).agg({
        'participant_id': 'nunique',
        'daily_steps': 'mean',
        'calories_burned': 'mean',
        'duration_minutes': 'mean',
        'distance_km': 'mean',
        'age': 'mean',
        'bmi': 'mean',
        'resting_heart_rate': 'mean',
        'season': 'first',
        'date': 'count'
    }).reset_index()

    weekly_group_stats.columns = [
        'group_id', 'year', 'week', 'member_count',
        'group_avg_steps_14', 'group_avg_kcal_14', 'group_avg_duration_14', 'group_avg_distance_14',
        'avg_age', 'avg_bmi', 'avg_resting_hr', 'season', 'active_days'
    ]

    # 7. group_weekly_active_rate 계산
    print("\n📈 group_weekly_active_rate 계산 중...")
    weekly_group_stats['group_weekly_active_rate'] = (weekly_group_stats['active_days'] / 7).round(2)

    # 8. 이전 주 달성률 (임시)
    print("\n🎯 이전 주 달성률 계산 중...")
    np.random.seed(42)
    weekly_group_stats['last_week_achievement_rate'] = np.random.uniform(0.5, 1.2, len(weekly_group_stats)).round(2)

    # 9. user_min 값 계산
    weekly_group_stats['user_min_steps'] = (weekly_group_stats['group_avg_steps_14'] * 0.6).round()
    weekly_group_stats['user_min_kcal'] = (weekly_group_stats['group_avg_kcal_14'] * 0.6).round(1)
    weekly_group_stats['user_min_duration'] = (weekly_group_stats['group_avg_duration_14'] * 0.6).round()
    weekly_group_stats['user_min_distance'] = (weekly_group_stats['group_avg_distance_14'] * 0.6).round(2)

    # 10. 🔑 핵심: 다음 주 실제 달성값을 타겟으로 사용
    print("\n🤖 실제 역사적 개선률 기반 타겟 생성 중...")
    weekly_group_stats = weekly_group_stats.sort_values(['group_id', 'year', 'week']).reset_index(drop=True)

    # 각 그룹의 다음 주 실제 달성값
    for metric in ['steps', 'kcal', 'duration', 'distance']:
        weekly_group_stats[f'next_week_{metric}'] = weekly_group_stats.groupby('group_id')[f'group_avg_{metric}_14'].shift(-1)

    # 다음 주 데이터가 있는 행만 유지
    weekly_group_stats = weekly_group_stats.dropna(subset=['next_week_steps']).copy()

    # 목표 = 다음 주 실제 달성값 × 그룹 인원
    for metric in ['steps', 'kcal', 'duration', 'distance']:
        weekly_group_stats[f'next_goal_{metric}'] = (
            weekly_group_stats[f'next_week_{metric}'] *
            weekly_group_stats['member_count']
        ).round(2 if metric == 'distance' else 0)

    # 개선률 통계
    for metric in ['steps', 'kcal', 'duration', 'distance']:
        improvement_rate = (weekly_group_stats[f'next_week_{metric}'] /
                           weekly_group_stats[f'group_avg_{metric}_14'])
        print(f"   {metric.capitalize()} 평균 개선률: {improvement_rate.mean():.3f}")

    # 11. 학습 데이터 저장
    print("\n💾 학습 데이터 저장 중...")

    # Steps
    steps_cols = [
        'member_count', 'group_avg_steps_14', 'last_week_achievement_rate',
        'avg_age', 'avg_bmi', 'avg_resting_hr', 'season',
        'group_weekly_active_rate', 'user_min_steps', 'next_goal_steps'
    ]
    training_steps = weekly_group_stats[steps_cols].dropna()
    training_steps.to_csv(PROCESSED_DATA_DIR / "training_steps.csv", index=False)
    print(f"   ✅ training_steps.csv 저장 완료 ({len(training_steps)} rows)")

    # Kcal
    kcal_cols = [
        'member_count', 'group_avg_kcal_14', 'last_week_achievement_rate',
        'avg_age', 'avg_bmi', 'avg_resting_hr', 'season',
        'group_weekly_active_rate', 'user_min_kcal', 'next_goal_kcal'
    ]
    training_kcal = weekly_group_stats[kcal_cols].dropna()
    training_kcal.to_csv(PROCESSED_DATA_DIR / "training_kcal.csv", index=False)
    print(f"   ✅ training_kcal.csv 저장 완료 ({len(training_kcal)} rows)")

    # Duration
    duration_cols = [
        'member_count', 'group_avg_duration_14', 'last_week_achievement_rate',
        'avg_age', 'avg_bmi', 'avg_resting_hr', 'season',
        'group_weekly_active_rate', 'user_min_duration', 'next_goal_duration'
    ]
    training_duration = weekly_group_stats[duration_cols].dropna()
    training_duration.to_csv(PROCESSED_DATA_DIR / "training_duration.csv", index=False)
    print(f"   ✅ training_duration.csv 저장 완료 ({len(training_duration)} rows)")

    # Distance
    distance_cols = [
        'member_count', 'group_avg_distance_14', 'last_week_achievement_rate',
        'avg_age', 'avg_bmi', 'avg_resting_hr', 'season',
        'group_weekly_active_rate', 'user_min_distance', 'next_goal_distance'
    ]
    training_distance = weekly_group_stats[distance_cols].dropna()
    training_distance.to_csv(PROCESSED_DATA_DIR / "training_distance.csv", index=False)
    print(f"   ✅ training_distance.csv 저장 완료 ({len(training_distance)} rows)")

    print("\n✨ 전처리 완료! 모델이 실제 역사적 개선 패턴을 학습합니다.")

if __name__ == "__main__":
    create_training_data()

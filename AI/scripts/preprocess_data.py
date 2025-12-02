"""
v6 데이터 전처리 (Policy-based Pre-training)

핵심 개념:
- 정책 기반 권장 성장률 예측 (recommended_growth_rate: 0.95~1.10)
- 3주 롤링 통계 활용 (trend, level, stability)
- 실제 달성률 데이터 없이도 학습 가능
- groupby().rolling() 최적화로 10~100배 속도 향상
"""
import pandas as pd
import numpy as np
from pathlib import Path
from config import (
    RAW_DATA_DIR, PROCESSED_DATA_DIR,
    STEPS_FEATURES, KCAL_FEATURES, DURATION_FEATURES, DISTANCE_FEATURES,
    POLICY_WEIGHTS, TARGET_RANGE
)


def normalize_feature(series):
    """Min-Max 정규화 (0~1 범위)"""
    min_val = series.min()
    max_val = series.max()
    if max_val == min_val:
        return pd.Series(0.5, index=series.index)
    return (series - min_val) / (max_val - min_val)


def calculate_recommended_growth_rate(df, metric):
    """
    정책 기반 권장 성장률 계산

    Args:
        df: weekly_group DataFrame
        metric: 'steps', 'kcal', 'duration', 'distance'

    Returns:
        recommended_growth_rate (0.95~1.10)
    """
    # 1. Trend (상승 추세): 최근 주 / 3주 평균
    trend = df[f'group_{metric}_total'] / df[f'group_{metric}_mean_3w']
    trend = trend.replace([np.inf, -np.inf], 1.0).fillna(1.0)

    # 2. Level (활동 수준): 3주 평균 (높을수록 완화)
    level = df[f'group_{metric}_mean_3w']

    # 3. Stability (안정성): 3주 변동성 (낮을수록 좋음)
    stability = df[f'group_{metric}_var_3w']

    # 정규화 (0~1)
    trend_norm = normalize_feature(trend)
    level_norm = normalize_feature(level)
    stability_norm = normalize_feature(stability)

    # Signal 계산
    signal = (
        POLICY_WEIGHTS['trend'] * trend_norm +
        POLICY_WEIGHTS['level'] * level_norm -
        POLICY_WEIGHTS['stability'] * stability_norm
    )

    # 권장 성장률: 1.0 ± 10% signal
    growth_rate = 1.0 + 0.1 * signal

    # 범위 제한
    growth_rate = growth_rate.clip(TARGET_RANGE[0], TARGET_RANGE[1])

    return growth_rate


def create_training_data():
    """
    v6 학습 데이터 생성 파이프라인

    출력:
        - training_steps.csv (8 columns: 7 features + 1 target)
        - training_kcal.csv
        - training_duration.csv
        - training_distance.csv
    """
    print("=" * 70)
    print("v6 Feature Engineering - Policy-based Pre-training")
    print("=" * 70)

    # ============================================
    # Step 1: 데이터 로드 및 전처리
    # ============================================
    print("\n[Step 1/8] 데이터 로드 및 전처리")
    df = pd.read_csv(RAW_DATA_DIR / "health_fitness_dataset.csv")
    print(f"  원본 데이터: {df.shape}")

    # 필요한 컬럼만 선택
    keep_cols = [
        'participant_id', 'date', 'age', 'bmi',
        'daily_steps', 'calories_burned', 'duration_minutes', 'fitness_level'
    ]
    df = df[keep_cols].copy()

    # 날짜 변환
    df['date'] = pd.to_datetime(df['date'])

    # distance_km 계산 (평균 보폭 0.75m 기준)
    df['distance_km'] = df['daily_steps'] * 0.00075

    # 결측치 제거
    df = df.dropna()
    print(f"  전처리 후: {df.shape}")

    # ============================================
    # Step 2: 그룹 생성 (fitness_level 기준)
    # ============================================
    print("\n[Step 2/8] 그룹 생성 (2-6명, fitness_level 기준)")
    participants = df[['participant_id', 'fitness_level']].drop_duplicates().sort_values('fitness_level')

    np.random.seed(42)
    groups = []
    current_group = []
    group_id = 1

    for pid in participants['participant_id']:
        current_group.append(pid)

        if len(current_group) >= np.random.randint(2, 7):
            for member in current_group:
                groups.append({'participant_id': member, 'group_id': group_id})
            group_id += 1
            current_group = []

    # 남은 참가자 처리
    if current_group:
        for member in current_group:
            groups.append({'participant_id': member, 'group_id': group_id})

    group_df = pd.DataFrame(groups)
    df = df.merge(group_df, on='participant_id', how='left')

    print(f"  총 그룹 수: {df['group_id'].nunique()}")
    print(f"  그룹당 평균 인원: {df.groupby('group_id')['participant_id'].nunique().mean():.1f}명")

    # ============================================
    # Step 3: 주차별 그룹 집계
    # ============================================
    print("\n[Step 3/8] 주차별 그룹 집계")
    df['week'] = df['date'].dt.to_period('W-MON')

    weekly_group = df.groupby(['group_id', 'week']).agg({
        'participant_id': 'nunique',
        'age': 'mean',
        'bmi': 'mean',
        'daily_steps': 'sum',
        'calories_burned': 'sum',
        'duration_minutes': 'sum',
        'distance_km': 'sum'
    }).reset_index()

    weekly_group.rename(columns={
        'participant_id': 'member_count',
        'age': 'avg_age',
        'bmi': 'avg_bmi',
        'daily_steps': 'group_steps_total',
        'calories_burned': 'group_kcal_total',
        'duration_minutes': 'group_duration_total',
        'distance_km': 'group_distance_total'
    }, inplace=True)

    # 정렬 (rolling 연산 전 필수)
    weekly_group['week_start'] = weekly_group['week'].dt.start_time
    weekly_group = weekly_group.sort_values(['group_id', 'week_start'])

    print(f"  총 레코드: {len(weekly_group)}")

    # ============================================
    # Step 4: 구성원 간 편차 계산
    # ============================================
    print("\n[Step 4/8] 구성원 간 편차 계산")
    member_variance = df.groupby(['group_id', 'week']).agg({
        'daily_steps': 'std'
    }).reset_index()

    member_variance.rename(columns={'daily_steps': 'member_steps_var'}, inplace=True)
    member_variance['member_steps_var'] = member_variance['member_steps_var'].fillna(0)

    weekly_group = weekly_group.merge(member_variance, on=['group_id', 'week'], how='left')
    weekly_group['member_steps_var'] = weekly_group['member_steps_var'].fillna(0)

    # ============================================
    # Step 5: 3주 롤링 통계 계산 (최적화 버전) ⚡
    # ============================================
    print("\n[Step 5/8] 3주 롤링 통계 계산 (최적화 버전)")
    print("  방법: groupby().rolling() 직접 사용 (10~100배 빠름)")

    metrics = ['steps', 'kcal', 'duration', 'distance']

    for metric in metrics:
        # 3주 평균
        weekly_group[f'group_{metric}_mean_3w'] = (
            weekly_group.groupby('group_id')[f'group_{metric}_total']
            .rolling(window=3, min_periods=1)
            .mean()
            .reset_index(level=0, drop=True)
        )

        # 3주 변동성
        weekly_group[f'group_{metric}_var_3w'] = (
            weekly_group.groupby('group_id')[f'group_{metric}_total']
            .rolling(window=3, min_periods=1)
            .std()
            .fillna(0)
            .reset_index(level=0, drop=True)
        )

    print("  완료: 8개 컬럼 추가 (mean_3w × 4 + var_3w × 4)")

    # ============================================
    # Step 6: 정책 기반 타겟 생성
    # ============================================
    print("\n[Step 6/8] 정책 기반 타겟 생성 (recommended_growth_rate)")

    for metric in metrics:
        weekly_group[f'recommended_growth_rate_{metric}'] = calculate_recommended_growth_rate(
            weekly_group, metric
        )

    print("  정책 수식:")
    print("    signal = 0.4×trend + 0.3×level - 0.3×stability")
    print("    growth_rate = clip(1.0 + 0.1×signal, 0.95, 1.10)")

    for metric in metrics:
        col = f'recommended_growth_rate_{metric}'
        print(f"\n  {metric.upper()}:")
        print(f"    평균: {weekly_group[col].mean():.4f}")
        print(f"    범위: {weekly_group[col].min():.4f} ~ {weekly_group[col].max():.4f}")

    # ============================================
    # Step 7: 데이터 필터링 (3주 이상)
    # ============================================
    print("\n[Step 7/8] 데이터 필터링 (3주 이상 데이터 있는 그룹만)")
    weekly_group_filtered = weekly_group.groupby('group_id').filter(lambda x: len(x) >= 3)

    print(f"  필터 전: {len(weekly_group)} rows")
    print(f"  필터 후: {len(weekly_group_filtered)} rows")

    # ============================================
    # Step 8: CSV 파일 저장 (4개 메트릭)
    # ============================================
    print("\n[Step 8/8] CSV 파일 저장")

    # Steps
    training_steps = weekly_group_filtered[STEPS_FEATURES + ['recommended_growth_rate_steps']].copy()
    training_steps.rename(columns={'recommended_growth_rate_steps': 'recommended_growth_rate'}, inplace=True)
    training_steps.to_csv(PROCESSED_DATA_DIR / "training_steps.csv", index=False)
    print(f"  ✅ training_steps.csv: {training_steps.shape}")

    # Kcal
    training_kcal = weekly_group_filtered[KCAL_FEATURES + ['recommended_growth_rate_kcal']].copy()
    training_kcal.rename(columns={'recommended_growth_rate_kcal': 'recommended_growth_rate'}, inplace=True)
    training_kcal.to_csv(PROCESSED_DATA_DIR / "training_kcal.csv", index=False)
    print(f"  ✅ training_kcal.csv: {training_kcal.shape}")

    # Duration
    training_duration = weekly_group_filtered[DURATION_FEATURES + ['recommended_growth_rate_duration']].copy()
    training_duration.rename(columns={'recommended_growth_rate_duration': 'recommended_growth_rate'}, inplace=True)
    training_duration.to_csv(PROCESSED_DATA_DIR / "training_duration.csv", index=False)
    print(f"  ✅ training_duration.csv: {training_duration.shape}")

    # Distance
    training_distance = weekly_group_filtered[DISTANCE_FEATURES + ['recommended_growth_rate_distance']].copy()
    training_distance.rename(columns={'recommended_growth_rate_distance': 'recommended_growth_rate'}, inplace=True)
    training_distance.to_csv(PROCESSED_DATA_DIR / "training_distance.csv", index=False)
    print(f"  ✅ training_distance.csv: {training_distance.shape}")

    print("\n" + "=" * 70)
    print("✨ v6 전처리 완료!")
    print("   - 7개 Feature (3주 롤링 통계 포함)")
    print("   - 정책 기반 권장 성장률 (0.95~1.10)")
    print("   - 최적화된 groupby().rolling() 사용")
    print("=" * 70)


if __name__ == "__main__":
    create_training_data()

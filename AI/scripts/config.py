"""
프로젝트 전체 설정
"""
import os
from pathlib import Path

# 경로 설정
BASE_DIR = Path(__file__).parent.parent
DATA_DIR = BASE_DIR / "data"
RAW_DATA_DIR = DATA_DIR / "raw"
PROCESSED_DATA_DIR = DATA_DIR / "processed"
MODELS_DIR = BASE_DIR / "models"

# 모델 설정
MODEL_PARAMS = {
    'objective': 'regression',
    'metric': 'rmse',
    'learning_rate': 0.05,
    'num_leaves': 31,
    'num_boost_round': 200
}

# 피처 설정 (주차별 그룹 목표)
COMMON_FEATURES = [
    'member_count',              # 그룹 인원 수 (2-6명)
    'last_week_achievement_rate',  # 지난주 달성률
    'avg_age',                   # 그룹 평균 나이
    'avg_bmi',                   # 그룹 평균 BMI
    'avg_resting_hr',            # 그룹 평균 안정시 심박수
    'season',                    # 계절 (1~4)
    'group_weekly_active_rate',  # 주간 활동 비율 (활동일수/7일)
]

STEPS_FEATURES = COMMON_FEATURES + ['group_avg_steps_14', 'user_min_steps']
KCAL_FEATURES = COMMON_FEATURES + ['group_avg_kcal_14', 'user_min_kcal']
DURATION_FEATURES = COMMON_FEATURES + ['group_avg_duration_14', 'user_min_duration']
DISTANCE_FEATURES = COMMON_FEATURES + ['group_avg_distance_14', 'user_min_distance']

# 그룹 설정
GROUP_MIN_SIZE = 2
GROUP_MAX_SIZE = 6
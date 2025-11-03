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

# 피처 설정
COMMON_FEATURES = [
    'member_count',
    'last_week_achievement_rate',
    'avg_age',
    'avg_bmi',
    'avg_resting_hr',
    'season',
]

STEPS_FEATURES = COMMON_FEATURES + ['group_avg_steps_14', 'user_min_steps']
KCAL_FEATURES = COMMON_FEATURES + ['group_avg_kcal_14', 'user_min_kcal']
DURATION_FEATURES = COMMON_FEATURES + ['group_avg_duration_14', 'user_min_duration']
DISTANCE_FEATURES = COMMON_FEATURES + ['group_avg_distance_14', 'user_min_distance']
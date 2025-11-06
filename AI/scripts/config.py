"""
v6 프로젝트 전체 설정 (Policy-based Pre-training)
"""
from pathlib import Path

# 경로 설정
BASE_DIR = Path(__file__).parent.parent
DATA_DIR = BASE_DIR / "data"
RAW_DATA_DIR = DATA_DIR / "raw"
PROCESSED_DATA_DIR = DATA_DIR / "processed"
MODELS_DIR = BASE_DIR / "models"

# 모델 설정 (LightGBM)
MODEL_PARAMS = {
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

NUM_BOOST_ROUND = 200

# v6 Feature 설정 (7개)
COMMON_FEATURES = [
    'member_count',    # 그룹 인원 수 (2-6명)
    'avg_age',         # 그룹 평균 나이
    'avg_bmi',         # 그룹 평균 BMI
]

# 메트릭별 Feature (각 7개)
STEPS_FEATURES = COMMON_FEATURES + [
    'group_steps_mean_3w',    # 최근 3주 평균 걸음수
    'group_steps_var_3w',     # 최근 3주 걸음수 변동성
    'group_duration_mean_3w', # 최근 3주 평균 운동시간
    'member_steps_var'        # 구성원 간 걸음수 편차
]

KCAL_FEATURES = COMMON_FEATURES + [
    'group_kcal_mean_3w',     # 최근 3주 평균 칼로리
    'group_kcal_var_3w',      # 최근 3주 칼로리 변동성
    'group_duration_mean_3w', # 최근 3주 평균 운동시간
    'member_steps_var'        # 구성원 간 걸음수 편차
]

DURATION_FEATURES = COMMON_FEATURES + [
    'group_duration_mean_3w', # 최근 3주 평균 운동시간
    'group_duration_var_3w',  # 최근 3주 운동시간 변동성
    'group_steps_mean_3w',    # 최근 3주 평균 걸음수
    'member_steps_var'        # 구성원 간 걸음수 편차
]

DISTANCE_FEATURES = COMMON_FEATURES + [
    'group_distance_mean_3w', # 최근 3주 평균 거리
    'group_distance_var_3w',  # 최근 3주 거리 변동성
    'group_duration_mean_3w', # 최근 3주 평균 운동시간
    'member_steps_var'        # 구성원 간 걸음수 편차
]

# 그룹 설정
GROUP_MIN_SIZE = 2
GROUP_MAX_SIZE = 6

# 정책 기반 타겟 설정
POLICY_WEIGHTS = {
    'trend': 0.4,      # 추세 가중치
    'level': 0.3,      # 수준 가중치 (음수)
    'stability': 0.3   # 안정성 가중치 (음수)
}

TARGET_RANGE = (0.95, 1.10)  # 권장 성장률 범위

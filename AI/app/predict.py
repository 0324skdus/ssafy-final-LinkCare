import threading
from functools import lru_cache
from pathlib import Path
import lightgbm as lgb
import numpy as np

# 모델 파일 경로 (컨테이너 실행 시 -v 마운트로 연결)
MODEL_DIR = Path(__file__).parent / "models"

MODEL_PATHS = {
    "steps": MODEL_DIR / "goal_steps_model.txt",
    "kcal": MODEL_DIR / "goal_kcal_model.txt",
    "duration": MODEL_DIR / "goal_duration_model.txt",
    "distance": MODEL_DIR / "goal_distance_model.txt",
}

FEATURES = {
    "steps": [
        "member_count", "avg_age", "avg_bmi",
        "group_steps_mean_3w", "group_steps_var_3w",
        "group_duration_mean_3w", "member_steps_var"
    ],
    "kcal": [
        "member_count", "avg_age", "avg_bmi",
        "group_kcal_mean_3w", "group_kcal_var_3w",
        "group_duration_mean_3w", "member_steps_var"
    ],
    "duration": [
        "member_count", "avg_age", "avg_bmi",
        "group_duration_mean_3w", "group_duration_var_3w",
        "group_steps_mean_3w", "member_steps_var"
    ],
    "distance": [
        "member_count", "avg_age", "avg_bmi",
        "group_distance_mean_3w", "group_distance_var_3w",
        "group_duration_mean_3w", "member_steps_var"
    ],
}

_reload_lock = threading.Lock()


@lru_cache(maxsize=1)
def load_models() -> dict:
    """모든 모델을 1회 로드 후 캐싱"""
    models = {}
    for key, path in MODEL_PATHS.items():
        models[key] = lgb.Booster(model_file=str(path))
    return models


def reload_models():
    """모델 캐시 초기화 후 재로드"""
    with _reload_lock:
        load_models.cache_clear()
        load_models()


def predict_goal(metric: str, payload: dict) -> float:
    """metric에 맞는 모델 선택 후 예측"""
    models = load_models()
    if metric not in models:
        raise ValueError(f"Invalid metric: {metric}")
    model = models[metric]
    X = np.array([[payload[f] for f in FEATURES[metric]]], dtype=float)
    y = float(model.predict(X)[0])
    return max(0.95, min(1.10, y))

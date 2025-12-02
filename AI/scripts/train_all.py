"""
v6 모델 학습 (Policy-based Pre-training)

4개 메트릭(Steps, Kcal, Duration, Distance)에 대한
권장 성장률 예측 모델을 학습합니다.
"""
import pandas as pd
import lightgbm as lgb
from pathlib import Path
from config import (
    PROCESSED_DATA_DIR, MODELS_DIR,
    STEPS_FEATURES, KCAL_FEATURES, DURATION_FEATURES, DISTANCE_FEATURES,
    MODEL_PARAMS, NUM_BOOST_ROUND
)


def train_model(metric_name, features, target_col='recommended_growth_rate'):
    """
    단일 메트릭 모델 학습

    Args:
        metric_name: 'steps', 'kcal', 'duration', 'distance'
        features: feature 리스트 (7개)
        target_col: 타겟 컬럼명 (기본: 'recommended_growth_rate')

    Returns:
        LightGBM Booster 모델
    """
    print(f"\n{'='*70}")
    print(f"🎯 {metric_name.upper()} 모델 학습")
    print(f"{'='*70}")

    # 1. 데이터 로드
    print("\n[1/5] 데이터 로드")
    csv_path = PROCESSED_DATA_DIR / f"training_{metric_name}.csv"
    df = pd.read_csv(csv_path)
    print(f"  데이터 크기: {df.shape}")
    print(f"  Feature 수: {len(features)}")

    # 2. Train/Valid 분할 (80/20)
    print("\n[2/5] Train/Valid 분할 (80/20)")
    train_size = int(len(df) * 0.8)
    train_df = df[:train_size]
    valid_df = df[train_size:]

    X_train = train_df[features]
    y_train = train_df[target_col]
    X_valid = valid_df[features]
    y_valid = valid_df[target_col]

    print(f"  Train: {X_train.shape}")
    print(f"  Valid: {X_valid.shape}")

    # 3. LightGBM Dataset 생성
    print("\n[3/5] LightGBM Dataset 생성")
    lgb_train = lgb.Dataset(X_train, y_train)
    lgb_valid = lgb.Dataset(X_valid, y_valid, reference=lgb_train)

    # 4. 모델 학습
    print("\n[4/5] 모델 학습 중...")
    print(f"  Parameters:")
    for key, value in MODEL_PARAMS.items():
        print(f"    - {key}: {value}")
    print(f"  num_boost_round: {NUM_BOOST_ROUND}")

    model = lgb.train(
        MODEL_PARAMS,
        lgb_train,
        num_boost_round=NUM_BOOST_ROUND,
        valid_sets=[lgb_train, lgb_valid],
        valid_names=['train', 'valid']
    )

    # 5. 모델 저장
    print("\n[5/5] 모델 저장")
    model_path = MODELS_DIR / f"goal_{metric_name}_model.txt"
    model.save_model(str(model_path))
    print(f"  ✅ 저장 완료: {model_path}")

    # Feature Importance
    print("\n📊 Feature Importance (Top 5):")
    importance = model.feature_importance(importance_type='gain')
    feature_importance = pd.DataFrame({
        'feature': features,
        'importance': importance
    }).sort_values('importance', ascending=False)

    for idx, row in feature_importance.head(5).iterrows():
        print(f"  {row['feature']}: {row['importance']:.0f}")

    # 성능 평가
    print("\n📈 성능 평가:")
    train_score = model.best_score['train']['rmse']
    valid_score = model.best_score['valid']['rmse']
    print(f"  Train RMSE: {train_score:.6f}")
    print(f"  Valid RMSE: {valid_score:.6f}")

    # 예측 범위 확인
    y_pred = model.predict(X_valid)
    print(f"\n🎲 예측 범위:")
    print(f"  Min: {y_pred.min():.4f}")
    print(f"  Max: {y_pred.max():.4f}")
    print(f"  Mean: {y_pred.mean():.4f}")

    return model


def train_all_models():
    """4개 메트릭 모델을 모두 학습"""
    print("=" * 70)
    print("🚀 v6 전체 모델 학습 시작")
    print("   - Policy-based Pre-training")
    print("   - 타겟: recommended_growth_rate (0.95~1.10)")
    print("=" * 70)

    models = {}

    # Steps
    models['steps'] = train_model('steps', STEPS_FEATURES)

    # Kcal
    models['kcal'] = train_model('kcal', KCAL_FEATURES)

    # Duration
    models['duration'] = train_model('duration', DURATION_FEATURES)

    # Distance
    models['distance'] = train_model('distance', DISTANCE_FEATURES)

    print("\n" + "=" * 70)
    print("✅ 전체 모델 학습 완료!")
    print("=" * 70)
    print("\n저장된 모델:")
    for metric in ['steps', 'kcal', 'duration', 'distance']:
        model_path = MODELS_DIR / f"goal_{metric}_model.txt"
        print(f"  - {model_path}")

    return models


if __name__ == "__main__":
    train_all_models()

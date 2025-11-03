"""
Steps 모델 학습
"""
import lightgbm as lgb
import pandas as pd
from config import PROCESSED_DATA_DIR, MODELS_DIR, MODEL_PARAMS, STEPS_FEATURES

def train_steps_model():
    # 1. 데이터 로드
    df = pd.read_csv(PROCESSED_DATA_DIR / "training_steps.csv")
    
    # 2. X, y 분리
    X = df[STEPS_FEATURES]
    y = df['next_goal_steps']
    
    # 3. 학습
    train_data = lgb.Dataset(X, label=y)
    model = lgb.train(MODEL_PARAMS, train_data)
    
    # 4. 저장
    model.save_model(MODELS_DIR / "goal_steps_model.txt")
    print("✅ Steps 모델 저장 완료")

if __name__ == "__main__":
    train_steps_model()
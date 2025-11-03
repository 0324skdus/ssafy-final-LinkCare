"""
4개 모델 한번에 학습
"""
from train_steps import train_steps_model
from train_kcal import train_kcal_model
from train_duration import train_duration_model
from train_distance import train_distance_model

if __name__ == "__main__":
    print("🚀 전체 모델 학습 시작...")
    
    print("\n1️⃣ Steps 모델 학습")
    train_steps_model()
    
    print("\n2️⃣ Kcal 모델 학습")
    train_kcal_model()
    
    print("\n3️⃣ Duration 모델 학습")
    train_duration_model()
    
    print("\n4️⃣ Distance 모델 학습")
    train_distance_model()
    
    print("\n✅ 전체 학습 완료!")
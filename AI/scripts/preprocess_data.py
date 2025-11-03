"""
원본 데이터 → 학습용 데이터 변환
"""
import pandas as pd
from pathlib import Path
from config import RAW_DATA_DIR, PROCESSED_DATA_DIR

def get_season(date_str):
    """날짜 → 계절 변환"""
    pass

def create_training_data():
    """
    health_fitness_dataset.csv 읽어서
    → training_steps.csv, training_kcal.csv 등 생성
    """
    pass

if __name__ == "__main__":
    create_training_data()
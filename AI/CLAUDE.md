# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LinkCare AI is a machine learning service that generates group healthcare goals using LightGBM regression models. The system predicts personalized fitness targets (steps, calories, duration, distance) based on group dynamics and historical performance data.

## Architecture

### Core Components

# CLAUDE.md

## Project Overview

LinkCare AI는 LightGBM 회귀 모델을 사용하여 **그룹 단위 주간 헬스케어 목표**를 생성하는 머신러닝 서비스입니다. 그룹원들의 최근 14일 활동 데이터와 지난주 달성률을 기반으로 다음 주 목표(걸음수, 칼로리, 운동시간, 거리)를 예측합니다.

## Architecture

### Core Components

1. **Data Pipeline** (`scripts/`)
   - `config.py`: 경로, 모델 파라미터, 피처 세트 설정
   - `preprocess_data.py`: 원본 데이터 → 학습 데이터 변환 ✅
     - 불필요한 컬럼 제거
     - fitness_level 기준 2-6명 랜덤 그룹 생성
     - 주차별 집계 및 group_weekly_active_rate 계산
   - `train_steps.py`, `train_kcal.py`, `train_duration.py`, `train_distance.py`: 개별 모델 학습 ✅
   - `train_all.py`: 4개 모델 일괄 학습 ✅

2. **Models** (`models/`)
   - LightGBM 모델 (`.txt` 형식)
   - 현재: `fitlife_steps_model.txt` (실험용)
   - 목표: `goal_steps_model.txt`, `goal_kcal_model.txt`, `goal_duration_model.txt`, `goal_distance_model.txt`

3. **API Service** (`app/`)
   - FastAPI 서버 (개발 예정)
   - 엔드포인트: `/predict_goals` (4개 목표 예측 → 랜덤 선택)

4. **Data Storage** (`data/`)
   - `raw/`: 원본 데이터 (`health_fitness_dataset.csv`)
   - `processed/`: 학습용 CSV (생성 예정)
     - `training_steps.csv`
     - `training_kcal.csv`
     - `training_duration.csv`
     - `training_distance.csv`

5. **Notebooks** (`notebooks/`)
   - `01_data_exploration.ipynb`: 데이터 탐색 및 시각화
   - `02_feature_engineering.ipynb`: 피처 생성 및 학습 데이터 생성 ⭐
   - `03_model_training.ipynb`: 모델 학습 및 평가
   - `fitlife_steps.ipynb`: 초기 실험 (참고용)

## Development Workflow

### 현재 개발 단계
1. notebooks/02_feature_engineering.ipynb 수정 중 
   → data/processed/ 에 4개 CSV 생성

2. notebooks/03_model_training.ipynb 실행  
   → models/ 에 4개 모델 생성

3. scripts/ 자동화 (노트북 → 스크립트 변환)

4. app/ FastAPI 서버 구축

5. Docker 패키징 및 EC2 배포

### Commands

#### Environment Setup
```bash
# 가상환경 활성화
.venv\Scripts\activate  # Windows
source .venv/bin/activate  # Unix

# 의존성 설치
pip install -r requirements.txt
```

#### Data Processing
```bash
# 스크립트로 학습 데이터 생성
python scripts/preprocess_data.py
# → data/processed/training_*.csv 4개 파일 생성
```

#### Model Training
```bash
# 전체 모델 학습
python scripts/train_all.py

# 또는 개별 모델 학습
python scripts/train_steps.py
python scripts/train_kcal.py
python scripts/train_duration.py
python scripts/train_distance.py
```

#### (참고) Notebook 사용
```bash
# Jupyter 실행
jupyter notebook

# 02_feature_engineering.ipynb: 데이터 탐색 및 피처 실험
# 03_model_training.ipynb: 모델 평가 및 튜닝
```

#### (향후) API Server
```bash
uvicorn app.main:app --reload
```

## Feature Engineering

### 데이터 전처리 (preprocess_data.py)

1. **불필요한 컬럼 제거**
   - 유지: participant_id, date, age, bmi, resting_heart_rate, daily_steps, calories_burned, duration_minutes, fitness_level
   - 제거: height_cm, weight_kg, activity_type, intensity, avg_heart_rate, hours_sleep, stress_level, hydration_level, blood_pressure_*, health_condition, smoking_status, gender

2. **그룹 생성 전략**
   - fitness_level 기준으로 참가자 정렬
   - 랜덤 크기 (2-6명) 그룹 생성
   - 유사한 운동 수준의 사람들이 같은 그룹에 배치되어 학습 효율성 향상

3. **주차별 집계**
   - 일별 데이터 → 주차별(week) 그룹 데이터로 변환
   - 목표는 주 단위 그룹 목표 (개인 목표 아님)

### 입력 피처 (9개, 모든 모델 공통)

| 피처명 | 설명 | 타입 | 계산 방법 |
|--------|------|------|-----------|
| `member_count` | 그룹 인원 수 | int | 2-6명 (랜덤) |
| `group_avg_[metric]_14` | 그룹 주간 평균 | float | 주차별 그룹 평균 |
| `last_week_achievement_rate` | 지난주 달성률 | float | 실제 달성 / 지난주 목표 |
| `avg_age` | 그룹 평균 나이 | float | SUM(age) / 인원수 |
| `avg_bmi` | 그룹 평균 BMI | float | SUM(bmi) / 인원수 |
| `avg_resting_hr` | 그룹 평균 안정시 심박수 | float | SUM(resting_heart_rate) / 인원수 |
| `season` | 계절 (1~4) | int | 1=봄, 2=여름, 3=가을, 4=겨울 |
| `group_weekly_active_rate` | 주간 활동 비율 | float | 활동 일수 / 7일 |
| `user_min_[metric]` | 사용자 최소 기준 | float | 그룹 평균 * 0.6 (임시) |

### 출력 (타겟)
- `next_goal_steps`: 다음 주 그룹 걸음수 목표
- `next_goal_kcal`: 다음 주 그룹 칼로리 목표
- `next_goal_duration`: 다음 주 그룹 운동시간 목표
- `next_goal_distance`: 다음 주 그룹 거리 목표

## Model Configuration
```python
MODEL_PARAMS = {
    'objective': 'regression',
    'metric': 'rmse',
    'learning_rate': 0.05,
    'num_leaves': 31,
    'num_boost_round': 200
}
```

## Integration Context

LinkCare 모노레포 구조:
- **AI 서버**: `C:/Users/SSAFY/Desktop/LinkCareAI` (이 프로젝트)
- **백엔드**: `C:/Users/SSAFY/Desktop/LinkCare/S13P31A307` (Spring Boot/Kotlin)
- **프론트엔드**: Android 앱 (Kotlin + Jetpack Compose)

백엔드가 AI 서버의 `/predict_goals` API를 호출하여 목표 생성.

## Current Status

### ✅ 완료
- 데이터 탐색 및 피처 설계
- 노트북 기반 파이프라인 구축
- `.gitignore` 설정
- **scripts/ 자동화 구현**
  - preprocess_data.py: 불필요 컬럼 제거, 2-6명 랜덤 그룹 생성, 주차별 집계
  - train_*.py: 4개 메트릭 모델 학습 스크립트 완성
  - config.py: 9개 피처 설정 (group_weekly_active_rate 포함)

### ⏳ 진행 중
- 학습 데이터 생성 테스트
- 모델 학습 및 평가

### 📋 예정
- FastAPI 서버 구축
- Docker 패키징
- EC2 배포
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LinkCare AI는 LightGBM 회귀 모델을 사용하여 **그룹 단위 주간 헬스케어 목표**를 생성하는 머신러닝 서비스입니다. **정책 기반 사전 학습(Policy-based Pre-training)** 방식을 통해 실제 달성률 데이터 없이도 그룹의 상태를 분석하여 적정한 **권장 성장률(recommended_growth_rate)**을 예측합니다.

## Architecture (v6)

### Core Components

1. **Data Pipeline** (`notebooks/`)
   - `02_feature_engineering.ipynb`: 정책 기반 학습 데이터 생성 ⭐
     - Raw data → 그룹 주간 집계 → 3주 이동 통계 → 정책 기반 타겟 생성
     - 4개 CSV 파일 생성 (steps, duration, kcal, distance)

2. **Models** (`models/`)
   - LightGBM 회귀 모델 (`.txt` 형식)
   - `goal_steps_model.txt`: 걸음수 권장 성장률 예측
   - `goal_duration_model.txt`: 운동시간 권장 성장률 예측
   - `goal_kcal_model.txt`: 칼로리 권장 성장률 예측
   - `goal_distance_model.txt`: 거리 권장 성장률 예측

3. **API Service** (`app/`)
   - FastAPI 서버 (Python 3.12 + FastAPI 0.110.0)
   - `main.py`: FastAPI 애플리케이션 및 엔드포인트 정의
   - `predict.py`: LightGBM 모델 로드 및 예측 로직
   - `schemas.py`: Pydantic 요청/응답 스키마
   - 엔드포인트: 4개 메트릭별 권장 성장률 예측 (`/predict/steps`, `/predict/kcal`, `/predict/duration`, `/predict/distance`)

4. **Data Storage** (`data/`)
   - `raw/`: 원본 데이터 (`health_fitness_dataset.csv`)
   - `processed/`: 학습용 CSV
     - `training_steps.csv` (8 columns: 7 features + 1 target)
     - `training_duration.csv` (8 columns)
     - `training_kcal.csv` (8 columns)
     - `training_distance.csv` (8 columns)

5. **Notebooks** (`notebooks/`)
   - `01_data_exploration.ipynb`: 데이터 탐색 및 시각화
   - `02_feature_engineering.ipynb`: v6 정책 기반 학습 데이터 생성 ⭐
   - `03_model_training.ipynb`: v6 모델 학습 및 평가 ⭐

## Development Workflow

### 현재 개발 단계 (v6)

1. **데이터 준비**: `notebooks/02_feature_engineering.ipynb` 실행
   → `data/processed/`에 4개 CSV 생성 (정책 기반 권장 성장률)

2. **모델 학습**: `notebooks/03_model_training.ipynb` 실행
   → `models/`에 4개 모델 생성

3. **API 서버 구축**: FastAPI 서버 개발 완료 ✅
   → Python 3.12 환경 구축 및 서버 테스트 완료

4. **배포**: Docker 패키징 및 EC2 배포 (진행 예정)

5. **Fine-tuning**: 실제 데이터 수집 후 정책 보정 (미래)

### Commands

#### Environment Setup

**Python 버전 요구사항**: Python 3.12 (권장)
- Python 3.13은 일부 패키지(numpy, pandas 등)의 wheel 파일이 아직 제공되지 않아 소스 빌드 필요
- Python 3.12는 모든 패키지의 미리 컴파일된 wheel 지원으로 빠른 설치 가능

```bash
# Python 3.12 설치 확인
py -3.12 --version  # Windows
python3.12 --version  # Unix

# 가상환경 생성 (최초 1회)
py -3.12 -m venv .venv  # Windows
python3.12 -m venv .venv  # Unix

# 가상환경 활성화
.venv\Scripts\activate  # Windows
source .venv/bin/activate  # Unix

# 의존성 설치
pip install -r requirements.txt
```

#### Data Processing & Model Training
```bash
# Jupyter 노트북 실행
jupyter notebook

# 1. 02_feature_engineering.ipynb 실행 (전체 셀 실행)
#    → data/processed/training_*.csv 4개 파일 생성

# 2. 03_model_training.ipynb 실행 (전체 셀 실행)
#    → models/goal_*_model.txt 4개 모델 생성
```

#### API Server
```bash
# FastAPI 서버 실행
uvicorn app.main:app --reload

# 또는 백그라운드에서 실행
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

**사용 가능한 엔드포인트**:
- `GET /health` - 헬스 체크
- `POST /predict/steps` - 걸음수 권장 성장률 예측
- `POST /predict/kcal` - 칼로리 권장 성장률 예측
- `POST /predict/duration` - 운동시간 권장 성장률 예측
- `POST /predict/distance` - 거리 권장 성장률 예측
- `POST /reload_model` - 모델 재로드

## Feature Engineering (v6)

### 핵심 개념: 정책 기반 권장 성장률 예측

**v6 방식** (Policy-based Pre-training):
- **타겟**: `recommended_growth_rate` (0.95~1.10)
- **정책 수식**:
  ```python
  signal = (
      0.4 * normalize(trend_3w) +      # 상승세 → 목표 상향
      0.3 * normalize(level_3w) -      # 높은 수준 → 목표 완화
      0.3 * normalize(stability_3w)    # 변동 큼 → 목표 완화
  )
  recommended_growth_rate = clip(1.0 + 0.1 * signal, 0.95, 1.10)
  ```
- **장점**:
  - 달성률 데이터 없이도 학습 가능
  - 규칙 기반 정책을 모델이 자동화
  - 추후 실제 데이터로 fine-tuning 가능 (상위 호환)

### 데이터 전처리 단계

#### 1. 불필요한 컬럼 제거 및 파생 변수 생성
- **유지**: participant_id, date, age, bmi, daily_steps, calories_burned, duration_minutes, fitness_level
- **계산**: distance_km = daily_steps * 0.00075 (평균 보폭 0.75m 기준)
- **제거**: height_cm, weight_kg, activity_type, intensity, avg_heart_rate, hours_sleep, stress_level, hydration_level, blood_pressure_*, health_condition, smoking_status, gender, resting_heart_rate

#### 2. 그룹 생성 전략
- fitness_level 기준으로 참가자 정렬
- 랜덤 크기 (2-6명) 그룹 생성
- 유사한 운동 수준의 사람들이 같은 그룹에 배치

#### 3. 개인별 → 그룹별 주간 집계
- 일별 데이터 → 개인별 주간 집계 → 그룹별 주간 평균/분산
- 목표는 주 단위 그룹 목표 (개인 목표 아님)

#### 4. 3주 이동 통계 계산 (핵심!)
- `group_*_mean_3w`: 최근 3주 평균 (추세 파악)
- `group_*_var_3w`: 최근 3주 변동성 (안정성 측정)
- 최소 3주 데이터가 있는 행만 학습 데이터로 사용

#### 5. 정책 기반 타겟 생성
- 각 메트릭별로 추세(trend), 수준(level), 안정성(stability) 계산
- 정책 수식으로 권장 성장률 생성 (0.95~1.10)

### 입력 피처 (7개, 모든 모델 공통)

| 피처명 | 설명 | 타입 | 계산 방법 |
|--------|------|------|-----------|
| `member_count` | 그룹 인원 수 | int | 2-6명 (랜덤) |
| `avg_age` | 그룹 평균 나이 | float | SUM(age) / 인원수 |
| `avg_bmi` | 그룹 평균 BMI | float | SUM(bmi) / 인원수 |
| `group_steps_mean_3w` | 최근 3주 평균 걸음수 ⭐ | float | rolling(3).mean() |
| `group_steps_var_3w` | 최근 3주 걸음수 변동성 ⭐ | float | rolling(3).var() |
| `group_duration_mean_3w` | 최근 3주 평균 운동시간 ⭐ | float | rolling(3).mean() |
| `member_steps_var` | 구성원 간 걸음수 편차 | float | std(개인별 걸음수) |

**⭐ = v6 핵심 Feature (3주 이동 통계)**

**❌ 제거된 Feature** (v5 → v6):
- `season` (계절) - 성능 기여도 낮음
- `avg_resting_hr` (안정시 심박수) - 성능 기여도 낮음
- 개인 추세 Feature 4개 (avg_growth_rate_members, var_growth_rate_members, avg_achievement_members, trend_achievement_members) - 정책 기반 방식에서는 불필요

### 출력 (타겟)
- `recommended_growth_rate_steps`: 권장 걸음수 성장률 (0.95~1.10)
- `recommended_growth_rate_duration`: 권장 운동시간 성장률 (0.95~1.10)
- `recommended_growth_rate_kcal`: 권장 칼로리 성장률 (0.95~1.10)
- `recommended_growth_rate_distance`: 권장 거리 성장률 (0.95~1.10)

**해석**:
- 1.00 = 현재 수준 유지
- 1.05 = 5% 성장 권장 (상향)
- 0.98 = 2% 완화 권장 (하향)

**실제 목표 계산**: `next_goal = current_mean_3w * recommended_growth_rate`

## Model Configuration

```python
params = {
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

num_boost_round = 200
```

## Notebooks 설명 (v6)

### 01_data_exploration.ipynb
**목적**: 원본 데이터 탐색 및 시각화 (v6와 무관, 참고용)

**주요 내용**:
- 데이터 로드 및 기본 통계 확인
- 결측치 분석
- 분포 시각화 (걸음수, 칼로리, 운동시간)
- 상관관계 분석

### 02_feature_engineering.ipynb ⭐
**목적**: v6 정책 기반 학습 데이터 생성

**주요 단계**:
1. **Part 1**: 데이터 정제 (불필요한 컬럼 제거, 날짜 변환)
2. **Part 2**: 그룹 생성 (fitness_level 기준, 2-6명)
3. **Part 3**: 개인별 주간 집계
4. **Part 4**: 그룹별 주간 집계 (평균, 표준편차)
5. **Part 5**: 3주 이동 통계 계산 (핵심!)
6. **Part 6**: 7개 Feature 확인
7. **Part 7**: 정책 기반 타겟 생성 (핵심!)
8. **Part 8**: 4개 CSV 파일 생성
9. **Part 9**: 데이터 검증 및 시각화

**출력**:
- `data/processed/training_steps.csv` (8 columns)
- `data/processed/training_duration.csv` (8 columns)
- `data/processed/training_kcal.csv` (8 columns)
- `data/processed/training_distance.csv` (8 columns)
- 시각화 이미지 (feature_distributions.png, target_distribution.png, correlation_matrix.png)

**중요**: 이 노트북을 실행하면 모든 학습 데이터가 재생성됩니다.

### 03_model_training.ipynb ⭐
**목적**: v6 정책 기반 모델 학습

**주요 내용**:
- **Part 1**: 데이터 로드
- **Part 2**: 모델 설정 (LightGBM params)
- **Part 3-6**: 4개 모델 학습 (Steps, Duration, Kcal, Distance)
- **Part 7**: 모델 저장
- **Part 8**: 종합 평가 (성능 비교, Feature Importance)
- **Part 9**: 샘플 예측 테스트

**출력**:
- `models/goal_steps_model.txt`
- `models/goal_duration_model.txt`
- `models/goal_kcal_model.txt`
- `models/goal_distance_model.txt`

## Integration Context

LinkCare 모노레포 구조:
- **AI 서버**: `C:/Users/SSAFY/Desktop/LinkCare/S13P31A307/AI` (이 프로젝트)
- **백엔드**: `C:/Users/SSAFY/Desktop/LinkCare/S13P31A307` (Spring Boot/Kotlin)
- **프론트엔드**: Android 앱 (Kotlin + Jetpack Compose)

백엔드가 AI 서버의 API를 호출:
1. **입력**: 그룹 상태 (7개 feature)
   - `member_count`: 그룹 인원 수
   - `avg_age`: 평균 나이
   - `avg_bmi`: 평균 BMI
   - `group_steps_mean_3w`: 최근 3주 평균 걸음수
   - `group_steps_var_3w`: 최근 3주 걸음수 변동성
   - `group_duration_mean_3w`: 최근 3주 평균 운동시간
   - `member_steps_var`: 구성원 간 걸음수 편차

2. **엔드포인트**: 메트릭별 개별 호출
   - `POST /predict/steps` → 걸음수 권장 성장률
   - `POST /predict/kcal` → 칼로리 권장 성장률
   - `POST /predict/duration` → 운동시간 권장 성장률
   - `POST /predict/distance` → 거리 권장 성장률

3. **출력**: `{"predicted_growth_rate": 1.05}` (예: 5% 성장 권장)

4. **백엔드 로직**: `next_goal = current_mean_3w * predicted_growth_rate`

## Current Status

### ✅ 완료 (v6)
- v6 방식 설계 완료
- 정책 기반 타겟 생성 수식 구현
- 3주 이동 통계 Feature 추가
- 노트북 기반 파이프라인 구축 (02, 03)
- `.gitignore` 설정
- **Python 3.12 환경 구축 완료** (2025-11-05)
  - 기존 Python 3.13 가상환경 → Python 3.12로 재구축
  - requirements.txt 전체 패키지 설치 완료 (컴파일 없이 wheel 설치)
- **FastAPI 서버 구현 완료** (2025-11-05)
  - `app/main.py`: 5개 엔드포인트 구현 (health, predict/steps, predict/kcal, predict/duration, predict/distance)
  - `app/predict.py`: LightGBM 모델 로드 및 예측 로직
  - `app/schemas.py`: Pydantic 스키마 정의
  - 서버 실행 테스트 완료 (uvicorn)

### ⏳ 진행 중
- 노트북 실행 및 데이터 생성
- 모델 학습 및 검증

### 📋 예정
- Docker 패키징
- EC2 배포
- 실제 데이터 수집 후 fine-tuning (v7)

## Important Notes

### 노트북 실행 순서
1. `01_data_exploration.ipynb` (선택, 데이터 확인용)
2. `02_feature_engineering.ipynb` ⭐ (필수, 학습 데이터 생성)
3. `03_model_training.ipynb` ⭐ (필수, 모델 학습)

### 데이터 재생성 방법
`02_feature_engineering.ipynb` 전체 셀 실행 → `data/processed/` 파일 모두 재생성

### 스크립트 자동화 상태
**주의**: 현재 `scripts/` 디렉토리는 v6 방식이 적용되지 않았습니다.
- v6 검증 후 노트북 → 스크립트 변환 예정

### 주요 변경 사항 요약 (v5 → v6)

| 항목 | v5 (OLD) | v6 (NEW) |
|------|---------|---------|
| **철학** | 실제 데이터 기반 | 정책 기반 사전 학습 |
| **Feature 수** | 12개 | 7개 |
| **3주 이동 통계** | ❌ 없음 | ✅ 3개 추가 |
| **개인 추세 Feature** | ✅ 4개 | ❌ 제거 |
| **타겟명** | `growth_rate_*` | `recommended_growth_rate_*` |
| **타겟 계산** | 실제 성장률 | 정책 수식 기반 |
| **타겟 범위** | 0.5~1.5 | 0.95~1.10 (더 보수적) |
| **CSV 컬럼 수** | 13개 | 8개 |
| **Fine-tuning** | ❌ 불가 | ✅ 상위 호환 구조 |

### v6 핵심 이점

1. **Cold Start 해결**: 실제 달성률 데이터 없이도 학습 가능
2. **도메인 지식 반영**: 정책 수식이 전문가 규칙을 반영
3. **상위 호환 구조**: 추후 실제 데이터로 fine-tuning 가능
4. **단순하고 명확**: 7개 feature로 해석 가능성 향상
5. **보수적 예측**: 0.95~1.10 범위로 급격한 변화 방지

## Troubleshooting

### Python 3.13에서 패키지 설치 실패 (numpy, pandas, scikit-learn 등)
**원인**: Python 3.13은 최신 버전이라 일부 패키지의 미리 컴파일된 wheel 파일이 아직 제공되지 않음
**증상**:
- `Building wheel for numpy` 메시지 후 에러
- `error: Microsoft Visual C++ 14.0 or greater is required`
- `error: can't find Rust compiler` 등

**해결 방법** (권장):
1. Python 3.12 설치
2. 기존 가상환경 삭제: `rmdir /s /q .venv` (Windows) 또는 `rm -rf .venv` (Unix)
3. Python 3.12로 가상환경 재생성: `py -3.12 -m venv .venv`
4. 패키지 재설치: `.venv/Scripts/python.exe -m pip install -r requirements.txt`

**대체 방법** (비권장, 시간 소요):
- Microsoft C++ Build Tools 설치 + Rustup 설치하여 빌드 환경 구축

### 노트북 실행 시 컬럼 오류
**원인**: 기존 v5 CSV 파일이 남아있을 수 있음
**해결**: `02_feature_engineering.ipynb` 전체 셀 재실행

### 모델 예측 범위가 이상함
**원인**: v5 모델 사용 중
**해결**: `03_model_training.ipynb` 실행하여 v6 모델 재학습

### Feature 수가 맞지 않음
**원인**: 잘못된 데이터 로드
**해결**: v6 CSV 파일 확인 (8 columns: 7 features + 1 target)

### FastAPI 서버 실행 시 모듈 import 오류
**원인**: 가상환경이 활성화되지 않았거나 패키지가 설치되지 않음
**해결**:
1. 가상환경 활성화 확인: `.venv\Scripts\activate`
2. 패키지 설치 확인: `pip list` (fastapi, uvicorn, lightgbm 등이 있어야 함)
3. 필요시 재설치: `pip install -r requirements.txt`

## Next Steps

1. **노트북 실행**: 02 → 03 순서로 실행하여 v6 데이터 및 모델 생성
2. **성능 검증**: 모델 성능 확인 (R², RMSE 등)
3. **Docker 패키징**: Dockerfile 작성 및 이미지 빌드
4. **EC2 배포**: AWS EC2 인스턴스에 Docker 컨테이너 배포
5. **백엔드 통합**: Spring Boot 백엔드와 API 연동 테스트
6. **Fine-tuning** (장기): 실제 사용자 데이터 수집 후 정책 보정

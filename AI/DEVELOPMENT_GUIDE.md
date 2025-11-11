# LinkCare AI 개발 가이드

> **최종 수정**: 2025-01-11
> **버전**: v6

---

## 📋 목차

1. [환경 설정](#1-환경-설정)
2. [데이터 처리 및 모델 학습](#2-데이터-처리-및-모델-학습)
3. [API 서버 실행](#3-api-서버-실행)
4. [API 테스트](#4-api-테스트)
5. [트러블슈팅](#5-트러블슈팅)

---

## 1. 환경 설정

### 1.1 Python 버전 요구사항

**권장**: Python 3.12 (필수)

**이유**:
- Python 3.13은 일부 패키지(numpy, pandas 등)의 wheel 파일이 아직 제공되지 않음
- Python 3.12는 모든 패키지의 미리 컴파일된 wheel 지원으로 빠른 설치 가능

**확인**:
```bash
# Windows
py -3.12 --version

# Unix/Linux
python3.12 --version
```

---

### 1.2 가상환경 생성

```bash
# 가상환경 생성 (최초 1회)
py -3.12 -m venv .venv  # Windows
python3.12 -m venv .venv  # Unix/Linux

# 가상환경 활성화
.venv\Scripts\activate  # Windows
source .venv/bin/activate  # Unix/Linux

# 활성화 확인 (프롬프트 앞에 (.venv) 표시)
```

---

### 1.3 패키지 설치

```bash
# pip 업그레이드
python -m pip install --upgrade pip

# 의존성 설치
pip install -r requirements.txt

# 설치 확인
pip list
```

**주요 패키지**:
- `lightgbm==4.5.0`: 모델 학습 및 예측
- `fastapi==0.110.0`: API 서버
- `uvicorn==0.27.1`: ASGI 서버
- `pandas==2.2.3`: 데이터 처리
- `numpy==2.1.3`: 수치 연산
- `scikit-learn==1.5.2`: 데이터 분할 및 평가
- `jupyter==1.1.1`: 노트북 실행

---

### 1.4 프로젝트 구조

```
AI/
├── data/
│   ├── raw/
│   │   └── health_fitness_dataset.csv          # 원본 데이터
│   └── processed/
│       ├── training_steps.csv                  # 걸음수 학습 데이터
│       ├── training_duration.csv               # 운동시간 학습 데이터
│       ├── training_kcal.csv                   # 칼로리 학습 데이터
│       └── training_distance.csv               # 거리 학습 데이터
│
├── notebooks/
│   ├── 01_data_exploration.ipynb               # 데이터 탐색
│   ├── 02_feature_engineering.ipynb            # 학습 데이터 생성 ⭐
│   └── 03_model_training.ipynb                 # 모델 학습 ⭐
│
├── models/
│   ├── goal_steps_model.txt                    # 걸음수 예측 모델
│   ├── goal_duration_model.txt                 # 운동시간 예측 모델
│   ├── goal_kcal_model.txt                     # 칼로리 예측 모델
│   └── goal_distance_model.txt                 # 거리 예측 모델
│
├── app/
│   ├── __init__.py
│   ├── main.py                                 # FastAPI 엔드포인트
│   ├── predict.py                              # 모델 로드 및 예측 로직
│   └── schemas.py                              # Pydantic 스키마
│
├── .venv/                                      # 가상환경 (gitignore)
├── .gitignore
├── requirements.txt
├── Dockerfile
├── PROJECT_OVERVIEW.md                         # 전체 개요 ⭐
├── DEVELOPMENT_GUIDE.md                        # 개발 가이드 (이 문서) ⭐
├── DEPLOYMENT_GUIDE.md                         # 배포 가이드 ⭐
└── README.md
```

---

## 2. 데이터 처리 및 모델 학습

### 2.1 Jupyter Notebook 실행

```bash
# 가상환경 활성화 확인
(.venv) $

# Jupyter 실행
jupyter notebook

# 브라우저가 자동으로 열림 (http://localhost:8888)
```

---

### 2.2 노트북 실행 순서

#### 순서 1: 데이터 탐색 (선택)
**파일**: `notebooks/01_data_exploration.ipynb`

**목적**: 원본 데이터 이해 및 시각화

**실행 방법**:
1. Jupyter에서 파일 열기
2. `Kernel > Restart & Run All` 실행
3. 결과 확인 (분포, 상관관계 등)

**출력**: 없음 (시각화만)

---

#### 순서 2: 학습 데이터 생성 (필수) ⭐
**파일**: `notebooks/02_feature_engineering.ipynb`

**목적**: 정책 기반 학습 데이터 생성

**실행 방법**:
1. Jupyter에서 파일 열기
2. `Kernel > Restart & Run All` 실행 (약 2-3분 소요)
3. 진행 상황 확인

**출력**:
- `data/processed/training_steps.csv` (8 컬럼)
- `data/processed/training_duration.csv` (8 컬럼)
- `data/processed/training_kcal.csv` (8 컬럼)
- `data/processed/training_distance.csv` (8 컬럼)

**검증**:
```bash
# 파일 생성 확인
ls -lh data/processed/

# 출력 예시:
# training_steps.csv      123K
# training_duration.csv   123K
# training_kcal.csv       123K
# training_distance.csv   123K
```

---

#### 순서 3: 모델 학습 (필수) ⭐
**파일**: `notebooks/03_model_training.ipynb`

**목적**: 4개 LightGBM 모델 학습 및 평가

**실행 방법**:
1. Jupyter에서 파일 열기
2. `Kernel > Restart & Run All` 실행 (약 3-5분 소요)
3. 학습 진행 상황 및 성능 확인

**출력**:
- `models/goal_steps_model.txt`
- `models/goal_duration_model.txt`
- `models/goal_kcal_model.txt`
- `models/goal_distance_model.txt`

**성능 지표**:
- RMSE (Root Mean Squared Error): 0.02-0.04 (낮을수록 좋음)
- R² (결정계수): 0.7-0.9 (높을수록 좋음)

**검증**:
```bash
# 모델 파일 생성 확인
ls -lh models/

# 출력 예시:
# goal_steps_model.txt      45K
# goal_duration_model.txt   45K
# goal_kcal_model.txt       45K
# goal_distance_model.txt   45K
```

---

### 2.3 데이터 재생성 방법

기존 학습 데이터를 삭제하고 재생성해야 하는 경우:

```bash
# 1. 기존 파일 삭제
rm data/processed/training_*.csv
rm models/goal_*_model.txt

# 2. 노트북 순서대로 재실행
#    - 02_feature_engineering.ipynb (전체 셀 실행)
#    - 03_model_training.ipynb (전체 셀 실행)
```

---

## 3. API 서버 실행

### 3.1 로컬 서버 실행

```bash
# 가상환경 활성화 확인
.venv\Scripts\activate  # Windows
source .venv/bin/activate  # Unix/Linux

# FastAPI 서버 실행 (개발 모드)
uvicorn app.main:app --reload

# 출력:
# INFO:     Uvicorn running on http://127.0.0.1:8000 (Press CTRL+C to quit)
# INFO:     Started reloader process [12345] using StatReload
# INFO:     Started server process [12346]
# INFO:     Waiting for application startup.
# INFO:     Application startup complete.
```

**옵션 설명**:
- `--reload`: 코드 변경 시 자동 재시작 (개발 중 편리)
- `--host 0.0.0.0`: 외부 접속 허용 (기본: 127.0.0.1)
- `--port 8000`: 포트 번호 (기본: 8000)

---

### 3.2 프로덕션 모드 실행

```bash
# 백그라운드 실행 (프로덕션)
uvicorn app.main:app --host 0.0.0.0 --port 8000 &

# 또는 프로세스 관리자 사용 (권장)
# - systemd (Linux)
# - PM2 (Node.js 환경)
# - supervisord
```

---

### 3.3 API 문서 확인

서버 실행 후 브라우저에서:

- **Swagger UI**: http://127.0.0.1:8000/docs
- **ReDoc**: http://127.0.0.1:8000/redoc

**Swagger UI에서 할 수 있는 것**:
- 모든 엔드포인트 목록 확인
- 요청/응답 스키마 확인
- 직접 API 테스트 (Try it out)

---

## 4. API 테스트

### 4.1 헬스 체크

```bash
# 서버 상태 확인
curl http://127.0.0.1:8000/health

# 응답:
{"status":"ok"}
```

---

### 4.2 걸음수 예측 테스트

```bash
curl -X POST http://127.0.0.1:8000/predict/steps \
  -H "Content-Type: application/json" \
  -d '{
    "member_count": 4,
    "avg_age": 35.2,
    "avg_bmi": 23.5,
    "group_steps_mean_3w": 224000.0,
    "group_steps_var_3w": 5000.0,
    "group_duration_mean_3w": 1260.0,
    "member_steps_var": 1200.0
  }'

# 응답:
{"predicted_growth_rate":1.027}
```

---

### 4.3 칼로리 예측 테스트

```bash
curl -X POST http://127.0.0.1:8000/predict/kcal \
  -H "Content-Type: application/json" \
  -d '{
    "member_count": 4,
    "avg_age": 35.2,
    "avg_bmi": 23.5,
    "group_kcal_mean_3w": 8400.0,
    "group_kcal_var_3w": 200.0,
    "group_duration_mean_3w": 1260.0,
    "member_steps_var": 1200.0
  }'

# 응답:
{"predicted_growth_rate":1.024}
```

---

### 4.4 운동시간 예측 테스트

```bash
curl -X POST http://127.0.0.1:8000/predict/duration \
  -H "Content-Type: application/json" \
  -d '{
    "member_count": 4,
    "avg_age": 35.2,
    "avg_bmi": 23.5,
    "group_duration_mean_3w": 1260.0,
    "group_duration_var_3w": 50.0,
    "group_steps_mean_3w": 224000.0,
    "member_steps_var": 1200.0
  }'

# 응답:
{"predicted_growth_rate":1.032}
```

---

### 4.5 거리 예측 테스트

```bash
curl -X POST http://127.0.0.1:8000/predict/distance \
  -H "Content-Type: application/json" \
  -d '{
    "member_count": 4,
    "avg_age": 35.2,
    "avg_bmi": 23.5,
    "group_distance_mean_3w": 168.0,
    "group_distance_var_3w": 3.75,
    "group_duration_mean_3w": 1260.0,
    "member_steps_var": 1200.0
  }'

# 응답:
{"predicted_growth_rate":1.027}
```

---

### 4.6 모델 재로드 테스트

모델 파일을 수정한 후 서버 재시작 없이 재로드:

```bash
curl -X POST http://127.0.0.1:8000/reload_model

# 응답:
{"status":"reloaded"}
```

---

## 5. 트러블슈팅

### 5.1 Python 3.13에서 패키지 설치 실패

**증상**:
```
Building wheel for numpy...
error: Microsoft Visual C++ 14.0 or greater is required
```

**원인**: Python 3.13은 최신 버전이라 일부 패키지의 wheel 파일이 아직 제공되지 않음

**해결**:
1. Python 3.12 설치
2. 기존 가상환경 삭제: `rmdir /s /q .venv` (Windows) 또는 `rm -rf .venv` (Unix)
3. Python 3.12로 가상환경 재생성: `py -3.12 -m venv .venv`
4. 패키지 재설치: `pip install -r requirements.txt`

---

### 5.2 노트북 실행 시 컬럼 오류

**증상**:
```
KeyError: 'group_steps_mean_3w'
```

**원인**: 기존 v5 CSV 파일이 남아있을 수 있음

**해결**:
```bash
# 기존 파일 삭제
rm data/processed/training_*.csv

# 02_feature_engineering.ipynb 전체 셀 재실행
```

---

### 5.3 모델 예측 범위가 이상함

**증상**: 예측값이 0.95~1.10 범위를 벗어남

**원인**: v5 모델 사용 중

**해결**:
```bash
# 기존 모델 삭제
rm models/goal_*_model.txt

# 03_model_training.ipynb 실행하여 v6 모델 재학습
```

---

### 5.4 FastAPI 서버 실행 시 모듈 import 오류

**증상**:
```
ModuleNotFoundError: No module named 'fastapi'
```

**원인**: 가상환경이 활성화되지 않았거나 패키지가 설치되지 않음

**해결**:
```bash
# 1. 가상환경 활성화 확인
.venv\Scripts\activate  # Windows
source .venv/bin/activate  # Unix/Linux

# 2. 패키지 설치 확인
pip list | grep fastapi

# 3. 없으면 재설치
pip install -r requirements.txt
```

---

### 5.5 모델 파일을 찾을 수 없음

**증상**:
```
FileNotFoundError: models/goal_steps_model.txt
```

**원인**: 모델 파일이 생성되지 않음

**해결**:
```bash
# 1. 모델 파일 존재 확인
ls -la models/

# 2. 없으면 03_model_training.ipynb 실행
jupyter notebook

# 3. 03_model_training.ipynb 전체 셀 실행
```

---

### 5.6 Feature 수가 맞지 않음

**증상**:
```
ValueError: feature dimension mismatch, expected 7 columns, got X
```

**원인**: 잘못된 데이터 로드 또는 Feature 순서 불일치

**해결**:
1. `app/schemas.py`에서 요청 스키마 확인
2. `app/predict.py`의 `FEATURES` 순서 확인
3. 요청 JSON의 key가 정확한지 확인

**올바른 요청 예시** (Steps):
```json
{
  "member_count": 4,              // ✅ 순서 1
  "avg_age": 35.2,                // ✅ 순서 2
  "avg_bmi": 23.5,                // ✅ 순서 3
  "group_steps_mean_3w": 224000,  // ✅ 순서 4
  "group_steps_var_3w": 5000,     // ✅ 순서 5
  "group_duration_mean_3w": 1260, // ✅ 순서 6
  "member_steps_var": 1200        // ✅ 순서 7
}
```

---

### 5.7 Jupyter Kernel 오류

**증상**:
```
Kernel died, restarting...
```

**원인**: 메모리 부족 또는 패키지 충돌

**해결**:
```bash
# 1. Jupyter 재시작
jupyter notebook stop
jupyter notebook

# 2. 가상환경 재생성 (극단적인 경우)
deactivate
rm -rf .venv
python3.12 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
jupyter notebook
```

---

## 6. 추가 명령어

### 6.1 가상환경 비활성화

```bash
deactivate
```

---

### 6.2 설치된 패키지 목록 확인

```bash
pip list
```

---

### 6.3 특정 패키지 업그레이드

```bash
pip install --upgrade lightgbm
```

---

### 6.4 의존성 파일 재생성 (개발자용)

```bash
# 현재 설치된 패키지를 requirements.txt로 저장
pip freeze > requirements.txt
```

---

## 7. 다음 단계

개발이 완료되면 다음 문서를 참고하세요:

- **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)**: Docker 및 EC2 배포 가이드
- **[PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)**: 전체 프로젝트 개요

---

**작성일**: 2025-01-11
**최종 수정**: 2025-01-11
**버전**: v6.0

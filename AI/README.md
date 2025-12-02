# LinkCare AI

> **그룹 헬스케어 목표 생성 AI**
>
> LightGBM 기반 주간 목표 자동 생성 시스템

[![Python](https://img.shields.io/badge/Python-3.12-blue.svg)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110.0-green.svg)](https://fastapi.tiangolo.com/)
[![LightGBM](https://img.shields.io/badge/LightGBM-4.5.0-orange.svg)](https://lightgbm.readthedocs.io/)

---

## 📌 프로젝트 소개

LinkCare AI는 **그룹 단위의 최근 3주 운동 데이터**를 기반으로 다음 주의 **적정 목표 성장률**을 예측하는 머신러닝 서비스입니다.

### 핵심 기능
- 🎯 **4개 메트릭 독립 예측**: 걸음수, 운동시간, 칼로리, 이동거리
- 🚀 **Cold Start 해결**: 실제 달성 데이터 없이도 정책 기반 목표 생성
- 📊 **정책 기반 사전 학습**: 규칙 기반 타겟으로 학습 후 실데이터 fine-tuning 가능
- ⚡ **FastAPI 기반**: 고성능 REST API 서버

---

## 🚀 빠른 시작

### 1. 환경 설정

```bash
# Python 3.12 가상환경 생성
py -3.12 -m venv .venv  # Windows
python3.12 -m venv .venv  # Unix/Linux

# 가상환경 활성화
.venv\Scripts\activate  # Windows
source .venv/bin/activate  # Unix/Linux

# 패키지 설치
pip install -r requirements.txt
```

---

### 2. 데이터 생성 및 모델 학습

```bash
# Jupyter 노트북 실행
jupyter notebook

# 실행 순서:
# 1. notebooks/02_feature_engineering.ipynb (전체 셀 실행)
#    → data/processed/training_*.csv 생성
#
# 2. notebooks/03_model_training.ipynb (전체 셀 실행)
#    → models/goal_*_model.txt 생성
```

---

### 3. API 서버 실행

```bash
# FastAPI 서버 실행
uvicorn app.main:app --reload

# 서버 접속: http://127.0.0.1:8000
# API 문서: http://127.0.0.1:8000/docs
```

---

### 4. API 테스트

```bash
# 헬스 체크
curl http://127.0.0.1:8000/health

# 걸음수 예측
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

# 응답: {"predicted_growth_rate":1.027}
```

---

## 📂 프로젝트 구조

```
AI/
├── data/                    # 데이터
│   ├── raw/                 # 원본 데이터
│   └── processed/           # 학습용 CSV (4개)
│
├── notebooks/               # Jupyter 노트북
│   ├── 01_data_exploration.ipynb
│   ├── 02_feature_engineering.ipynb  ⭐ 학습 데이터 생성
│   └── 03_model_training.ipynb       ⭐ 모델 학습
│
├── models/                  # 학습된 모델 (4개)
│   ├── goal_steps_model.txt
│   ├── goal_duration_model.txt
│   ├── goal_kcal_model.txt
│   └── goal_distance_model.txt
│
├── app/                     # FastAPI 서버
│   ├── main.py              # 엔드포인트
│   ├── predict.py           # 예측 로직
│   └── schemas.py           # Pydantic 스키마
│
└── requirements.txt         # Python 의존성
```

---

## 🔌 API 엔드포인트

| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/health` | 헬스 체크 |
| `POST` | `/predict/steps` | 걸음수 권장 성장률 예측 |
| `POST` | `/predict/kcal` | 칼로리 권장 성장률 예측 |
| `POST` | `/predict/duration` | 운동시간 권장 성장률 예측 |
| `POST` | `/predict/distance` | 거리 권장 성장률 예측 |
| `POST` | `/reload_model` | 모델 재로드 |

**자세한 API 문서**: http://127.0.0.1:8000/docs

---

## 🧠 모델 정보

### 알고리즘
- **LightGBM** (Gradient Boosting Decision Tree)
- 회귀 모델 (Regression)

### 입력 (7개 Feature)
| Feature | 설명 | 타입 |
|---------|------|------|
| `member_count` | 그룹 인원 수 | int |
| `avg_age` | 평균 나이 | float |
| `avg_bmi` | 평균 BMI | float |
| `group_*_mean_3w` | 최근 3주 평균 (메트릭별) | float |
| `group_*_var_3w` | 최근 3주 변동성 (메트릭별) | float |
| `group_duration_mean_3w` | 최근 3주 평균 운동시간 | float |
| `member_steps_var` | 구성원 간 걸음수 편차 | float |

### 출력
- **권장 성장률** (0.95~1.10)
- 예: `1.027` = 다음 주 목표를 현재 평균 대비 **2.7% 증가** 권장

---

## 📚 문서

| 문서 | 설명 |
|------|------|
| **[PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)** | 프로젝트 전체 개요 (모델, 데이터, 아키텍처) ⭐ |
| **[DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)** | 개발 환경 설정 및 실행 가이드 ⭐ |
| **[DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)** | Docker 및 EC2 배포 가이드 ⭐ |

---

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| **언어** | Python 3.12 |
| **ML** | LightGBM 4.5.0, scikit-learn 1.5.2 |
| **API** | FastAPI 0.110.0, Uvicorn 0.27.1 |
| **데이터** | Pandas 2.2.3, NumPy 2.1.3 |
| **노트북** | Jupyter 1.1.1 |
| **배포** | Docker, AWS EC2 (예정) |

---

## 🗺️ 로드맵

### ✅ v6 (현재)
- 정책 기반 사전 학습 완료
- 4개 LightGBM 모델 학습
- FastAPI 서버 구축
- Python 3.12 환경 구축

### ⏳ 단기 (1-2주)
- Docker 이미지 빌드 및 테스트
- AWS EC2 배포
- Spring Boot 백엔드 연동

### 🎯 장기 (3-6개월)
- 실제 사용자 데이터 수집 (4주 이상)
- v7 Fine-tuning (실데이터 기반 학습)
- A/B 테스트 및 성능 개선

---

## 🤝 기여

이 프로젝트는 **SSAFY 13기 특화 프로젝트**의 일환으로 개발되었습니다.

**브랜치**:
- `AI_main`: AI 서버 개발 브랜치
- `main`: 전체 프로젝트 메인 브랜치

---

## 📝 라이선스

이 프로젝트는 교육 목적으로 제작되었습니다.

---

## 📞 문의

문제가 발생하거나 질문이 있으시면:
- 📚 **문서 확인**: [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)
- 🔧 **개발 가이드**: [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
- 🚀 **배포 가이드**: [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)

---

**최종 수정**: 2025-01-11
**버전**: v6.0

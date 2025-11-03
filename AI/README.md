# LinkCare AI

그룹 헬스케어 목표 생성 AI

## 구조
- `scripts/`: 데이터 전처리 및 모델 학습
- `app/`: FastAPI 서버
- `models/`: 학습된 모델

## 사용법

### 1. 데이터 전처리
```bash
python scripts/preprocess_data.py
```

### 2. 모델 학습
```bash
python scripts/train_all.py
```

### 3. 서버 실행
```bash
uvicorn app.main:app --reload
```

## API 사용
```bash
curl -X POST http://localhost:8000/predict_goals \
  -H "Content-Type: application/json" \
  -d '{...}'
```
```

---

## 🎯 **작업 순서**
```
1단계: 폴더 구조 생성
    ↓
2단계: config.py 작성
    ↓
3단계: preprocess_data.py 작성 (데이터 전처리)
    ↓
4단계: train_*.py 작성 (모델 학습)
    ↓
5단계: app/*.py 작성 (FastAPI 서버)
    ↓
6단계: 테스트 및 Docker 패키징
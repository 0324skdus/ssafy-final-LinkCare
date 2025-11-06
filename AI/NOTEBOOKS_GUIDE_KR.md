## LinkCare AI: 그룹 주간 목표 추천 모델 설계 요약 (v6)

### 1. 모델 개요

그룹 단위의 최근 운동 데이터를 기반으로, 다음 주의 **권장 성장률(recommended_growth_rate)** 을 예측하여 목표(steps, duration, distance, kcal)를 제시하는 모델.
현재는 **사전 학습(정책 기반 학습)** 단계이며, 추후 **실데이터 기반 학습(fine-tuning)** 으로 확장된다.

---

## 🔹 2. 모델 구성: 4개 독립 회귀 모델

| 모델 파일                     | 예측 대상      | 출력 의미                   | 활용 방식                                                 |
| ------------------------- | ---------- | ----------------------- | ----------------------------------------------------- |
| `goal_steps_model.txt`    | 걸음수 성장률    | 다음 주 목표 걸음수의 권장 성장 비율   | `next_goal_steps = mean_steps_3w * growth_rate`       |
| `goal_duration_model.txt` | 운동 시간 성장률  | 다음 주 목표 운동시간의 권장 성장 비율  | `next_goal_duration = mean_duration_3w * growth_rate` |
| `goal_distance_model.txt` | 이동 거리 성장률  | 러닝 중심 그룹의 거리 성장률 예측     | `next_goal_distance = mean_distance_3w * growth_rate` |
| `goal_kcal_model.txt`     | 소모 칼로리 성장률 | 체중 관리 중심 그룹의 칼로리 성장률 예측 | `next_goal_kcal = mean_kcal_3w * growth_rate`         |

> 4개의 모델은 **같은 입력(feature set)** 을 공유하며, 각 출력(y)만 다르다.
> 모든 모델은 “목표치”가 아니라 **권장 성장률(%)** 을 예측한다.

---

## 🔹 3. 사전 학습 (Policy-based Pre-training)

> 목표(goal)·달성(success) 데이터가 없는 상태에서, 규칙 기반으로 생성된 `y`를 학습시켜 **정책 함수의 패턴을 모델이 근사**하도록 한다.

### ● 입력(X)

| Feature                  | 설명            |
| ------------------------ | ------------- |
| `member_count`           | 그룹 인원수        |
| `avg_age`                | 평균 나이         |
| `avg_bmi`                | 평균 BMI        |
| `group_steps_mean_3w`    | 최근 3주 평균 걸음수  |
| `group_steps_var_3w`     | 최근 3주 걸음수 변동성 |
| `group_duration_mean_3w` | 최근 3주 평균 운동시간 |
| `member_steps_var`       | 구성원 간 편차      |

> 모든 모델(`steps`, `duration`, `distance`, `kcal`)이 동일한 feature 구조를 사용한다.

### ● 출력(y)

| 항목                                                                                         | 설명                   |
| ------------------------------------------------------------------------------------------ | -------------------- |
| `recommended_growth_rate`                                                                  | 0.95~1.10 범위의 권장 성장률 |
| 수식 예시                                                                                      | ```python            |
| signal = (0.4*normalize(trend_3w) + 0.3*normalize(level_3w) - 0.3*normalize(stability_3w)) |                      |
| recommended_growth_rate = np.clip(1.0 + 0.1*signal, 0.95, 1.10)                            |                      |

```|

### ● 학습 목적
- 규칙(정책 수식)의 계산 패턴을 모델이 자동화하도록 학습.  
- 목표가 없어도 **상태 기반으로 목표 강도(성장률)** 제시 가능.

### ● 출력 모델
- `goal_steps_model.txt`, `goal_duration_model.txt`, `goal_distance_model.txt`, `goal_kcal_model.txt`

---

## 🔹 4. 실데이터 기반 학습 (Real-data Fine-tuning)
> 실제 그룹 목표(`group_goals`)와 달성 여부(`group_goal_records.is_succeeded`)가 누적된 후, 정책 기반 모델을 현실 데이터로 보정.

### ● 입력(X)
| Feature | 설명 |
|----------|------|
| 기존 feature | 사전 학습과 동일한 feature 유지 |
| `goal_step_prev` | 이전 주 목표 |
| `goal_gap_ratio` | 실제 활동량 / 이전 주 목표 |
| `success_rate` | **최근 N주간 그룹 목표 달성 비율** (`is_succeeded`의 평균) |

> `success_rate`는 개인별 달성률이 아닌, **그룹 단위의 주차별 목표 달성 비율**로 정의된다.  
> 예: 최근 4주 중 3주 목표 달성 → `success_rate = 0.75`  
> 사전 학습의 feature 구조를 그대로 유지하며, 새로운 feature는 추가(addition) 방식으로 확장한다.

### ● 출력(y)
| 항목 | 설명 |
|------|------|
| `recommended_growth_rate` | 현실 데이터를 반영한 권장 성장률 (예: 1.02 = +2%) |
| **보조 신호 (학습용)** | `actual_growth_rate` 또는 `goal_success_score` — fine-tuning 시 정책 보정 신호로 활용 |

> 모델의 **최종 출력은 항상 권장 성장률**이며, `success_rate`와 같은 지표는 학습 보조 입력으로만 사용된다.

### ● 학습 방법
| 방식 | 설명 |
|------|------|
| **Warm start 재학습** | 기존 모델을 초기값으로 두고 현실 데이터로 재학습 (`init_model` 사용) |
| **Residual 보정** | `(actual_growth_rate - predicted_growth_rate)` 차이 학습 후 합산 |
| **앙상블 보정** | 정책 모델과 현실 모델을 가중 평균 (`0.4*policy + 0.6*actual`) |

### ● 출력 모델
- `goal_steps_model_v2.txt`, `goal_duration_model_v2.txt`, `goal_distance_model_v2.txt`, `goal_kcal_model_v2.txt`

---

## 🔹 5. 단계별 역할 요약

| 단계 | 출력(y) 의미 | 학습 목적 | 데이터 필요 |
|------|----------------|--------------|----------------|
| **사전 학습** | 정책 수식 기반 권장 성장률 | 규칙 함수 학습 (목표 없이 상태 기반) | 일상 활동 로그만 필요 |
| **실데이터 학습** | 현실 보정된 권장 성장률 | 실제 목표 달성 비율을 반영해 정책 보정 | `group_goal_records` 기반 데이터 필요 |

> 두 단계 모두 “다음 주 목표를 얼마나 상향 또는 완화할지(%)”를 계산하는 모델이다.  
> 실제 목표치는 `현재 평균 × 예측된 성장률`로 후처리하여 계산된다.

---

## 🔹 6. Feature Schema 규칙
| 구분 | 허용 | 설명 |
|------|------|------|
| **Feature 추가 (확장)** | ✅ 가능 | `goal_gap_ratio`, `success_rate` 등 현실 피처는 후속 단계에서 추가 가능. 기존 구조 유지 필수. |
| **Feature 삭제 (축소)** | ❌ 불가 | 입력 스키마 변경 시 기존 학습 패턴 무효화. |
| **Feature 의미 변경** | ⚠️ 주의 | 단위·스케일 변경 시 재학습 필요. |

---

## 🔹 7. 최종 출력 흐름
```

입력: 그룹 상태 feature → 4개 모델 병렬 예측
↓
출력: 각 항목별 권장 성장률
↓
후처리: 현재 평균 × 권장 성장률 → 다음 주 목표치
↓
결과 예시: {
"steps": 8650,
"duration": 46.2,
"distance": 5.3,
"kcal": 2180
}

```

---

## 🔹 8. 핵심 요약
- **4개 독립 모델**이 걸음수·운동시간·거리·칼로리를 각각 예측한다.  
- 모든 모델은 “목표치”가 아닌 **권장 성장률(%)** 을 출력한다.  
- **사전 학습:** 규칙 기반 데이터로 모델에 정책 함수를 학습.  
- **실데이터 학습:** `success_rate` 등 현실 정보를 반영해 정책을 보정.  
- 최종적으로 **정책의 방향성 + 현실 적응성**을 모두 갖춘 목표 생성 AI로 발전한다.
```

from pydantic import BaseModel

# 공통 응답 스키마
class GoalPredictResponse(BaseModel):
    predicted_growth_rate: float


# 1. 걸음수(steps)
class StepsRequest(BaseModel):
    member_count: int
    avg_age: float
    avg_bmi: float
    group_steps_mean_3w: float
    group_steps_var_3w: float
    group_duration_mean_3w: float
    member_steps_var: float


# 2. 칼로리(kcal)
class KcalRequest(BaseModel):
    member_count: int
    avg_age: float
    avg_bmi: float
    group_kcal_mean_3w: float
    group_kcal_var_3w: float
    group_duration_mean_3w: float
    member_steps_var: float


# 3. 운동시간(duration)
class DurationRequest(BaseModel):
    member_count: int
    avg_age: float
    avg_bmi: float
    group_duration_mean_3w: float
    group_duration_var_3w: float
    group_steps_mean_3w: float
    member_steps_var: float


# 4. 이동거리(distance)
class DistanceRequest(BaseModel):
    member_count: int
    avg_age: float
    avg_bmi: float
    group_distance_mean_3w: float
    group_distance_var_3w: float
    group_duration_mean_3w: float
    member_steps_var: float

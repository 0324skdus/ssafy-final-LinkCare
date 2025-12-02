from fastapi import FastAPI, HTTPException
from app.predict import predict_goal, reload_models
from app.schemas import (
    StepsRequest, KcalRequest, DurationRequest, DistanceRequest,
    GoalPredictResponse
)

app = FastAPI(title="LinkCare Multi-Goal Predictor", version="v2")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/predict/steps", response_model=GoalPredictResponse)
def predict_steps(req: StepsRequest):
    try:
        y = predict_goal("steps", req.model_dump())
        return GoalPredictResponse(predicted_growth_rate=y)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/predict/kcal", response_model=GoalPredictResponse)
def predict_kcal(req: KcalRequest):
    try:
        y = predict_goal("kcal", req.model_dump())
        return GoalPredictResponse(predicted_growth_rate=y)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/predict/duration", response_model=GoalPredictResponse)
def predict_duration(req: DurationRequest):
    try:
        y = predict_goal("duration", req.model_dump())
        return GoalPredictResponse(predicted_growth_rate=y)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/predict/distance", response_model=GoalPredictResponse)
def predict_distance(req: DistanceRequest):
    try:
        y = predict_goal("distance", req.model_dump())
        return GoalPredictResponse(predicted_growth_rate=y)
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/reload_model")
def reload():
    reload_models()
    return {"status": "reloaded"}

from fastapi import FastAPI
from pydantic import BaseModel
import math

app = FastAPI(title="Self-Healing DC ML Sidecar", version="1.0.0")

class TelemetryData(BaseModel):
    phi: float
    timestamp: int

@app.get("/health")
def health():
    return {"status": "UP"}

@app.post("/api/v1/score/{node_id}")
def calculate_risk(node_id: str, data: TelemetryData):
    # Sigmoidal statistical failure score
    # phi around 8.0 maps to ~0.50, phi > 14 maps to >0.90
    phi = data.phi
    score = 1.0 / (1.0 + math.exp(-0.5 * (phi - 8.0)))
    score = round(float(score), 4)

    return {
        "node_id": node_id,
        "phi": phi,
        "failure_risk_score": score,
        "status": "CRITICAL" if score >= 0.8 else ("ELEVATED" if score >= 0.5 else "NOMINAL")
    }

import asyncio
from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="PIAS Mock")

class MockConfig(BaseModel):
    mode: Literal["success", "unavailable", "slow", "reject"] = "slow"
    fail_first: int = 0
    delay_seconds: float = 10

config = MockConfig()
attempts = 0
accepted: dict[str, dict] = {}

class Submission(BaseModel):
    jobId: str
    consultationId: str
    idempotencyKey: str

@app.post("/mock/config")
def configure(value: MockConfig):
    global config, attempts
    config = value
    attempts = 0
    return config

@app.post("/pias/submissions")
async def submit(body: Submission):
    global attempts

    # Repeated submissions return the existing result.
    if body.idempotencyKey in accepted:
        return accepted[body.idempotencyKey]

    attempts += 1

    if config.mode == "unavailable" or attempts <= config.fail_first:
        raise HTTPException(
            status_code=503,
            detail="PIAS temporarily unavailable",
        )

    if config.mode == "reject":
        raise HTTPException(
            status_code=422,
            detail="Invalid consultation data",
        )

    if config.mode == "slow":
        await asyncio.sleep(config.delay_seconds)

    # Recheck after waiting in case another request completed.
    if body.idempotencyKey not in accepted:
        accepted[body.idempotencyKey] = {
            "status": "accepted",
            "reference": f"MOCK-{body.idempotencyKey}",
        }

    return accepted[body.idempotencyKey]

@app.get("/mock/state")
def state():
    return {"attempts": attempts, "accepted": accepted}

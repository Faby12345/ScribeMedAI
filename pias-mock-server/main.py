import asyncio
from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="PIAS Mock")

class MockConfig(BaseModel):
    mode: Literal["success", "unavailable", "slow", "reject"] = "unavailable"
    fail_first: int = 0
    delay_seconds: float = 10

config = MockConfig()
attempts = 0
accepted: dict[str, dict] = {}

class Submission(BaseModel):
    submission_id: str
    consultation_id: str

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
    if body.submission_id in accepted:
        return accepted[body.submission_id]

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
    if body.submission_id not in accepted:
        accepted[body.submission_id] = {
            "status": "accepted",
            "reference": f"MOCK-{body.submission_id}",
        }

    return accepted[body.submission_id]

@app.get("/mock/state")
def state():
    return {"attempts": attempts, "accepted": accepted}

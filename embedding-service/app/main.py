from contextlib import asynccontextmanager

from fastapi import FastAPI
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

MODEL_NAME = "intfloat/multilingual-e5-small"

embedding_model: SentenceTransformer | None = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    global embedding_model

    embedding_model = SentenceTransformer(MODEL_NAME)

    yield

    embedding_model = None


app = FastAPI(
    title="ScribeMed Embedding Service",
    version="0.1.0",
    lifespan=lifespan,
)


class EmbeddingRequest(BaseModel):
    texts: list[str] = Field(min_length=1)


class EmbeddingResponse(BaseModel):
    model: str
    dimensions: int
    embeddings: list[list[float]]


@app.get("/health")
def health():
    return {
        "status": "UP",
        "model": MODEL_NAME,
        "modelLoaded": embedding_model is not None,
    }


@app.post("/embeddings", response_model=EmbeddingResponse)
def create_embeddings(
    request: EmbeddingRequest,
) -> EmbeddingResponse:
    if embedding_model is None:
        raise RuntimeError("Embedding model is not loaded")

    embeddings = embedding_model.encode(
        request.texts,
        normalize_embeddings=True,
    )

    values = embeddings.tolist()

    return EmbeddingResponse(
        model=MODEL_NAME,
        dimensions=len(values[0]),
        embeddings=values,
    )

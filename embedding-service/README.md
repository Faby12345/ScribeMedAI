# ScribeMed Embedding Service

FastAPI microservice responsible for generating multilingual text embeddings for ScribeMed's RAG pipeline.

The service uses `intfloat/multilingual-e5-small` to transform document chunks and user questions into 384-dimensional vectors.

## Requirements

- Python 3.11+
- pip

## Run locally

Create and activate a virtual environment:

```bash
python3 -m venv .venv
source .venv/bin/activate
```

Install dependencies:

```bash
pip install -r requirements.txt
```

Start the application:

```bash
uvicorn app.main:app --reload --port 8000
```

The service will be available at:

```text
http://localhost:8000
```

Swagger documentation:

```text
http://localhost:8000/docs
```

## API

### Health check

```http
GET /health
```

Example response:

```json
{
  "status": "UP",
  "model": "intfloat/multilingual-e5-small",
  "modelLoaded": true
}
```

### Generate embeddings

```http
POST /embeddings
Content-Type: application/json
```

Example request:

```json
{
  "texts": [
    "query: Care este tratamentul schizofreniei?",
    "passage: Tratamentul schizofreniei presupune..."
  ]
}
```

Example response:

```json
{
  "model": "intfloat/multilingual-e5-small",
  "dimensions": 384,
  "embeddings": [
    [0.012, -0.043, 0.081],
    [0.018, -0.031, 0.076]
  ]
}
```

## E5 prefixes

Every input must include the appropriate prefix:

- Use `query:` for user questions.
- Use `passage:` for document chunks.

```text
query: Care este tratamentul schizofreniei?
passage: Tratamentul schizofreniei presupune...
```

## Project structure

```text
embedding-service/
├── app/
│   ├── __init__.py
│   └── main.py
├── .gitignore
├── README.md
└── requirements.txt
```

## Role in ScribeMed

```text
Spring Boot backend
→ Embedding Service
→ 384-dimensional embedding
→ PostgreSQL with pgvector
```

The embedding service only generates vectors. Document processing, retrieval, authorization and answer generation remain inside the ScribeMed backend.
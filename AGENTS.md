# ScribeMedAI Agent Instructions

## Project overview

ScribeMedAI is a medical documentation MVP that supports the following workflow:

1. A doctor selects or creates a patient.
2. The doctor creates a consultation.
3. The doctor confirms that the patient has been informed.
4. The doctor records or uploads consultation audio.
5. The system transcribes the audio asynchronously.
6. The system generates a structured clinical-note draft.
7. The doctor reviews and edits the draft.
8. The doctor explicitly approves the final document.
9. The approved document can be copied, printed, or exported as PDF.
10. The result is stored in the patient's consultation history.

This MVP is not a complete EHR and does not currently include SIUI integration, billing, scheduling, automated diagnosis, or clinical recommendations.

## Product language

* ScribeMedAI targets the Romanian market.
* All product-facing UI text, page metadata, validation messages, emails, exported document labels, notifications, and support/help copy must be written in Romanian by default.
* Do not introduce English user-facing text unless the user explicitly requests it or the text is a technical identifier that should not be translated.
* Keep code identifiers, API field names, database names, logs, and internal technical messages in English unless there is a clear project convention requiring Romanian.

## Required architecture reference

Before implementing, refactoring, reviewing, or proposing architectural changes, read:

* `docs/architecture/system-architecture.md`

Treat this document as the current architectural baseline and source of truth for system-level decisions.

Do not introduce an architectural change without:

1. identifying the existing decision;
2. explaining why it is insufficient;
3. describing the proposed change and its trade-offs;
4. obtaining explicit approval when the change affects system boundaries, security, data storage, tenancy, authentication, or AI processing.

## Repository structure

The repository currently uses:

```text
ScribeMedAI/
├── backend/
├── frontend/
├── infrastructure/
├── docs/
│   └── architecture/
│       └── system-architecture.md
└── AGENTS.md
```

Respect the existing directory names. Do not rename or reorganize the repository unless explicitly requested.

## Architectural rules

### Backend

* Spring Boot is the only business backend.
* Use a modular-monolith architecture for the MVP.
* Keep domain and application logic out of controllers.
* Separate modules by business capability.
* PostgreSQL is the primary database.
* Flyway manages database migrations.
* Every table containing tenant-owned or medical data must include `tenant_id`.
* Tenant isolation must be enforced in the application and repository layers.
* Do not introduce microservices, RabbitMQ, Kafka, Kubernetes, or a vector database unless explicitly approved.

### Frontend

* Next.js and React provide the user interface.
* The frontend should call same-origin `/api/*` endpoints.
* Requests under `/api/*` are proxied or routed to Spring Boot.
* Do not duplicate medical-domain or business logic in Next.js.
* Do not create a Next.js Route Handler for every Spring Boot endpoint.
* Route Handlers or Server Actions may be used only for clearly justified frontend-specific behavior.
* Never store authentication tokens in `localStorage`.

### Authentication and authorization

* Use opaque server-side sessions.
* Store the session identifier in an `HttpOnly`, `Secure`, and appropriate `SameSite` cookie.
* Spring Boot owns authentication and authorization.
* Authorization must be enforced both at the HTTP layer and in the service layer.
* Every resource lookup must verify tenant ownership.
* State-changing browser requests must include CSRF protection.
* Frontend route protection is a usability feature, not a security boundary.

### AI processing

* Transcription and clinical-note generation must run asynchronously.
* Do not perform long-running AI processing inside the initial HTTP request.
* Use the PostgreSQL-backed `processing_job` queue for the MVP.
* The worker may share the Spring Boot codebase but run as a separate process or container.
* AI providers must be accessed through internal provider interfaces.
* Provider-specific code must not leak into the medical domain.
* AI output must be structured JSON and validated against a strict schema.
* AI output is always a draft.
* Only an authenticated and authorized doctor may approve a document.
* Do not automatically approve, diagnose, prescribe, or generate clinical recommendations.

### Documents and audit

* Never overwrite previous document versions.
* An approved document must be immutable.
* Further corrections must create a new version.
* Preserve AI provider, model, prompt version, template version, source, author, and timestamps.
* Audit events are append-only from the application's perspective.
* Do not place transcript text, clinical-note content, prompts, patient names, diagnoses, medications, or audio contents in audit metadata or operational logs.

### Audio and file storage

* Store consultation audio in private object storage.
* Prefer direct browser uploads using short-lived presigned URLs.
* Do not include patient names, email addresses, or other direct identifiers in object keys.
* Audio is temporary and must be deleted according to the configured retention policy.
* Validate file type, signature, size, checksum, codec, and consultation ownership.
* Never expose permanent object-storage credentials to the browser.

### Privacy and security

* Treat all patient and consultation data as sensitive.
* Minimize the data collected and stored.
* Do not log medical content.
* Do not send medical data to analytics tools.
* Sanitize external-provider error messages before storing or returning them.
* Use encrypted connections and encrypted storage.
* Keep secrets in environment variables or a secret manager, never in source control.
* Preserve EU-region and data-protection constraints described in the architecture document.
* Before any real-patient pilot, the DPIA, retention policy, DPA, incident-response plan, backup restoration test, and tenant-isolation tests must be completed.

### Operational logging

* Use the backend's standard SLF4J logger for technical diagnostics when a log helps operators understand system behavior, failures, or important state transitions.
* Do not create custom logging abstractions unless there is a concrete operational requirement that SLF4J/Logback cannot satisfy.
* Keep logs concise and intentional. Do not add entry/exit logs or noisy debug logs around ordinary control flow.
* Prefer structured key-value context in messages, such as `tenantId`, `consultationId`, `jobId`, `provider`, `status`, `errorCode`, and `durationMs`.
* Never log transcript text, clinical-note content, prompts, patient names, diagnoses, medications, audio contents, session identifiers, cookies, tokens, passwords, or raw external-provider errors.
* Sanitize external-provider errors before logging. Log provider error code, HTTP status, provider request ID, duration, and a safe summary only.
* Use the request correlation ID already attached to backend logs instead of manually inventing request identifiers.
* Keep business/security audit events separate from operational logs.

## Implementation workflow

Before editing code:

1. Inspect the relevant repository area.
2. Read `docs/architecture/system-architecture.md`.
3. Identify the affected module and architectural constraints.
4. Reuse existing patterns and dependencies.
5. Avoid unrelated refactoring.

After editing code:

1. Run the relevant formatter and linter.
2. Run unit tests.
3. Run integration tests where persistence, security, tenancy, or external integrations are affected.
4. Run the production build for the affected application.
5. Summarize changed files, behavior, tests, assumptions, and unresolved risks.

Do not claim that a command, test, migration, or build succeeded unless it was actually executed successfully.

## Testing expectations

Backend changes should include, where applicable:

* unit tests;
* PostgreSQL integration tests with Testcontainers;
* authorization tests;
* tenant-isolation tests;
* state-transition tests;
* idempotency and retry tests;
* AI-provider contract tests.

Frontend changes should include, where applicable:

* component or feature tests;
* recording-flow tests;
* processing-state tests;
* editor and approval tests;
* end-to-end tests for critical workflows.

For medical and security-sensitive behavior, do not rely only on manual testing.

## Current implementation order

Unless explicitly instructed otherwise, follow this order:

1. Foundation: repository, Docker, PostgreSQL, Flyway, Spring Boot, Next.js, authentication, tenant, user.
2. Patients and consultations.
3. Audio capture and private object-storage upload.
4. PostgreSQL processing jobs, worker, transcription, SSE, retries.
5. Structured AI note generation, schema validation, document versions, review flags.
6. Doctor review, autosave, regeneration, approval, and audit.
7. PDF export, print, clipboard, feedback, and metrics.
8. Privacy, security, retention, backup, restore, observability, and pilot hardening.

Do not skip foundational authentication, authorization, tenant isolation, migrations, or audit controls to implement later features faster.

## Coding principles

* Prefer the simplest implementation that respects the architecture.
* Keep changes small and reviewable.
* Use explicit names and typed interfaces.
* Validate inputs at system boundaries.
* Keep transactions in the application/service layer.
* Make processing operations idempotent.
* Use optimistic locking where concurrent edits are possible.
* Return sanitized errors to clients.
* Avoid speculative abstractions.
* Do not add dependencies without a concrete need.
* Update documentation when behavior or an architectural decision changes.

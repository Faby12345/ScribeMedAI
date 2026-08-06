# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Primary users are Romanian doctors working in clinics or individual medical practices. They use ScribeMedAI during or immediately after a consultation to reduce manual clinical documentation work while keeping final medical responsibility and approval under their control.

Secondary users, if any, are not confirmed yet. Future work should not assume patient-facing, receptionist, admin, billing, or insurer workflows unless they are explicitly added.

## Product Purpose

ScribeMedAI helps a doctor turn consultation audio into a structured clinical-note draft that can be reviewed, edited, explicitly approved, copied, printed, exported as PDF, and stored in the patient's consultation history.

Success means the doctor can move from patient selection to approved documentation with less manual typing, without giving up review control, tenant isolation, privacy protections, or Romanian-language product behavior.

## Positioning

ScribeMedAI is a medical documentation MVP for the Romanian market, not a full EHR. Its current product position is focused on consultation capture, asynchronous transcription, structured draft generation, doctor review, approval, and document history.

Open decision: the durable market positioning, differentiation against competing medical scribes or EHR add-ons, and any commercial claims have not been confirmed. Future work must not invent testimonials, benchmark claims, integrations, pricing, certifications, or customer proof.

## Operating Context

The core workflow is:

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

The MVP is designed for responsive browser use on desktop, tablet, and mobile. The frontend is a Next.js web app; the business backend is Spring Boot. Product-facing UI text, metadata, validation messages, exported document labels, notifications, and help copy are Romanian by default.

## Capabilities and Constraints

Confirmed capabilities include authentication, tenant-aware users, patient management, consultation management, consultation audio handling, asynchronous processing jobs, transcription provider integration, draft generation direction, document review and approval direction, audit direction, and export direction.

ScribeMedAI explicitly does not currently include SIUI integration, billing, scheduling, automated diagnosis, automated prescribing, clinical recommendations, or a complete EHR feature set.

Architectural constraints to preserve:

- Spring Boot is the only business backend.
- The MVP uses a modular-monolith architecture.
- PostgreSQL is the primary database.
- Flyway manages database migrations.
- Tenant-owned and medical data must include tenant isolation.
- Authentication and authorization are owned by Spring Boot.
- Browser sessions use opaque server-side sessions in secure HttpOnly cookies.
- State-changing browser requests require CSRF protection.
- Transcription and clinical-note generation run asynchronously.
- The MVP uses a PostgreSQL-backed `processing_job` queue.
- AI providers are accessed through internal provider interfaces.
- AI output is structured JSON, schema-validated, and always a draft.
- Only an authenticated and authorized doctor may approve a document.
- Approved documents are immutable; corrections create new versions.
- Audit events are append-only from the application's perspective.
- Medical content, patient names, prompts, transcripts, diagnoses, medications, audio contents, secrets, cookies, tokens, and raw provider errors must not be logged.
- Audio is stored in private object storage, uses non-identifying object keys, and is temporary under a configured retention policy.

Open decision: pilot-specific retention periods, provider choices, deployment environment, commercial packaging, and proof/compliance readiness dates are not confirmed.

## Brand Commitments

The product name is ScribeMedAI. The product targets the Romanian market and should present as a professional, calm, trustworthy medical SaaS product.

Product-facing language is Romanian by default. Code identifiers, API fields, database names, logs, and internal technical messages remain English unless a project convention says otherwise.

The primary product color is a professional medium or deep blue suitable for a medical SaaS product. Semantic colors must map to meaning: blue for primary and informational states, green for success and approval, amber for warnings or review-required states, red for destructive actions, and slate or gray for neutral secondary actions.

Open decision: logo, final visual identity, typography beyond the current implementation, brand voice beyond Romanian professional medical SaaS, and approved marketing copy are not confirmed.

## Evidence on Hand

Repository evidence:

- `AGENTS.md` contains product, architecture, security, privacy, implementation, testing, and frontend design-system instructions.
- `docs/architecture/system-architecture.md` is the current architecture baseline and source of truth for system-level decisions.
- `frontend/` contains a Next.js application with Romanian metadata and shared UI primitives.
- `backend/` contains a Spring Boot application with modules for identity, tenancy, patients, consultations, audio, processing, transcription, audit, security, and Flyway migrations.

No testimonials, production customer names, clinical validation studies, pricing, regulatory approval claims, SIUI integration proof, or real-patient pilot evidence are confirmed in the repository.

## Product Principles

1. Keep the doctor in control: AI output is always a draft, and final approval is explicit.
2. Protect sensitive medical data by default: minimize collection, isolate tenants, avoid unsafe logs, and preserve Romanian/EU privacy expectations.
3. Prefer focused MVP workflows over EHR sprawl: build patient, consultation, audio, draft, review, approval, export, and history flows before adjacent administrative features.
4. Make clinical work faster without hiding risk: surface processing state, review requirements, document versions, and approval status clearly.
5. Preserve implementation discipline: use the existing Spring Boot modular monolith, Next.js frontend, PostgreSQL/Flyway persistence, asynchronous worker direction, and reusable design system.

## Accessibility & Inclusion

The product must support accessible keyboard navigation, visible focus states, sufficient contrast, semantic markup, accessible labels and errors, adequate touch targets, and intentional responsive behavior across desktop, tablet, and mobile.

Romanian is the default product language. Future user-facing flows should not require English comprehension unless the user explicitly requests English or the text is a technical identifier.

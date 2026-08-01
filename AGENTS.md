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

## Frontend design system

All frontend work must follow a consistent, reusable design system.

### Design direction

ScribeMedAI must look like a professional, calm, trustworthy medical SaaS product.

The interface must not resemble:

* generic AI-generated UI;
* a Tailwind starter template;
* a landing page;
* a collection of unrelated cards;
* an overly decorative startup dashboard.

Prefer:

* strong visual hierarchy;
* restrained styling;
* deliberate spacing;
* consistent alignment;
* subtle borders;
* minimal shadows;
* accessible interaction states;
* responsive layouts;
* reusable components.

### Brand color

Blue is the primary product color.

Use a professional medium or deep blue suitable for a medical SaaS product.

Do not use neon blue, purple-blue gradients, or highly saturated colors.

The primary color is used for:

* login;
* save;
* continue;
* submit;
* primary navigation actions;
* selected and active states.

Do not use blue indiscriminately for every action.

### Semantic colors

Use colors according to meaning:

* primary actions: blue;
* successful and approved states: green;
* warnings and review-required states: amber;
* destructive actions: red;
* neutral and secondary actions: slate or gray;
* informational states: blue.

Examples of destructive actions:

* logout;
* delete;
* permanently remove;
* revoke;
* cancel an irreversible operation.

Logout should be visibly destructive but should not dominate the primary workflow.

### Design tokens

Colors, spacing, radii, shadows, and typography should be defined through centralized semantic tokens.

Prefer token names such as:

```css
--background
--surface
--surface-muted
--foreground
--muted-foreground
--primary
--primary-hover
--primary-foreground
--border
--input
--ring
--success
--warning
--destructive
--destructive-hover
--destructive-foreground
```

Do not spread hardcoded color values across components.

Do not create page-specific color classes.

### Reusable components

Shared UI elements must be implemented as reusable components.

At minimum, keep reusable versions of:

* `Button`
* `Input`
* `Textarea`
* `Select`
* `Checkbox`
* `Label`
* `FormField`
* `Card`
* `Alert`
* `Badge`
* `Spinner`

The `Button` component should support semantic variants:

* `primary`
* `secondary`
* `outline`
* `ghost`
* `success`
* `warning`
* `destructive`

Do not repeat long button class lists in individual pages.

Do not create classes such as:

```css
.login-button
.dashboard-button
.red-logout-button
```

Use semantic component variants instead.

### Layout rules

Use a consistent application shell and page structure.

Pages should generally contain:

1. navigation;
2. page header;
3. page description or context;
4. primary action;
5. main content;
6. contextual secondary actions.

Do not place every section inside a card.

Cards should only be used when they provide meaningful grouping.

Avoid:

* excessive nested cards;
* oversized empty areas;
* inconsistent page padding;
* oversized headings;
* excessive border radius;
* decorative gradients;
* glassmorphism;
* heavy shadows;
* random icon usage;
* full-width forms without a usability reason.

### Forms

All forms must use consistent labels, control heights, spacing, focus states, and validation messages.

Do not use placeholders as replacements for labels.

Form controls must support:

* normal;
* hover;
* focus-visible;
* disabled;
* invalid;
* loading where applicable.

### Status presentation

Use consistent semantic states:

* draft: neutral;
* processing: blue;
* review required: amber;
* approved: green;
* failed: red;
* archived: muted gray.

Do not communicate status using color alone.

### Accessibility

All UI changes must preserve or improve:

* keyboard navigation;
* visible focus states;
* sufficient contrast;
* semantic markup;
* accessible labels;
* accessible errors;
* touch target size;
* responsive behavior.

Never remove focus outlines without providing an accessible replacement.

### Responsive behavior

Every page must be designed for desktop, tablet, and mobile.

Do not merely shrink desktop layouts.

Navigation, forms, tables, cards, and page actions must adapt intentionally for smaller screens.

### Implementation discipline

Before creating a new visual pattern:

1. inspect existing shared components;
2. determine whether an existing component can be extended;
3. reuse semantic tokens;
4. avoid duplicated Tailwind class strings;
5. avoid inline styles;
6. avoid unnecessary dependencies.

When changing global design tokens or shared components, inspect all affected pages for regressions.


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

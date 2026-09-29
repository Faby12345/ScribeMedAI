---
title: "ScribeMed AI - Arhitectura tehnică a prototipului MVP"
status: "MVP architecture baseline"
language: "ro"
---

> Document de arhitectură derivat din prezentarea PDF a MVP-ului. Acesta este documentul principal pentru agenții de cod și trebuie actualizat când se schimbă o decizie arhitecturală.

# ScribeMed AI - Arhitectura tehnică a prototipului MVP

## 1. Obiectivul arhitecturii

Arhitectura trebuie să susțină fluxul principal al produsului:

1. medicul selectează pacientul;
2. creează o consultație;
3. confirmă informarea pacientului;
4. înregistrează sau încarcă un fișier audio;
5. sistemul transcrie conversația;
6. sistemul generează un draft de fișă medicală;
7. medicul verifică și editează;
8. medicul aprobă documentul;
9. documentul poate fi copiat, exportat PDF sau tipărit;
10. rezultatul este salvat în istoricul pacientului.

Arhitectura este construită direct pe funcționalitățile și limitele prototipului descris: un singur utilizator principal, înregistrare audio, transcriere, structurare, editor, aprobare, export și feedback, fără SIUI, programări, facturare sau un EHR complet.

## 2. Principiile arhitecturale

### 2.1 Modular monolith, nu microservicii

Pentru MVP recomand un backend Spring Boot dezvoltat ca modular monolith.

Toate funcționalitățile se află inițial în aceeași aplicație și în aceeași bază de date, dar codul este separat în module logice:

- identitate și autentificare;
- clinici și utilizatori;
- pacienți;
- consultații;
- audio;
- procesare AI;
- documente;
- șabloane;
- bază de cunoștințe;
- audit;
- feedback.

Această variantă este mai simplă de dezvoltat, testat și instalat decât o arhitectură cu microservicii. Separarea internă permite extragerea ulterioară a workerului AI sau a generatorului de documente într-un serviciu separat, dacă volumul o va cere.

### 2.2 Procesare asincronă

Transcrierea și generarea documentului nu trebuie executate în requestul HTTP prin care medicul finalizează încărcarea audio.

Backendul salvează un job de procesare și răspunde imediat. Un worker separat preia jobul, transcrie fișierul și generează documentul.

Astfel:

- requesturile API nu rămân blocate;
- procesarea poate fi reluată după o eroare;
- utilizatorul poate închide sau reîncărca pagina;
- backendul rămâne disponibil în timpul procesării AI.

### 2.3 Medicul rămâne în control

Documentul generat de AI este întotdeauna un `DRAFT`.

AI-ul nu poate crea direct un document cu status `APPROVED`. Numai medicul autentificat poate aproba versiunea finală.

Documentul aprobat trebuie să păstreze:

- versiunea generată inițial de AI;
- modificările medicului;
- utilizatorul care a aprobat;
- data aprobării;
- modelul și configurația care au generat draftul.

### 2.4 Minimizarea datelor

Aplicația păstrează doar informațiile necesare MVP-ului.

Nu sunt obligatorii în prima versiune:

- CNP;
- card de sănătate;
- adresă completă;
- informații de asigurare;
- date SIUI;
- înregistrarea audio pe termen lung.

Audio-ul este temporar. Fișa aprobată și datele de audit pot avea o retenție mai lungă, stabilită contractual împreună cu clinica.

### 2.5 Izolarea clinicilor încă de la început

Chiar dacă primul prototip este testat de un singur medic, fiecare tabel care conține date medicale trebuie să aibă un tenant_id sau clinic_id.

Acest lucru previne o migrare dificilă atunci când aplicația începe să deservească mai multe cabinete.

## 3. Arhitectura generală

```text
┌───────────────────────────────────────────────────────────────┐
│                            Browser medic                                                 │
│                  Next.js Web App / PWA                                                   │
│                                                                                          │
│   Login • Pacienți • Înregistrare • Editor • PDF • Feedback                              │
└──────────────────────────┬────────────────────────────────────┘
│ HTTPS
▼
┌───────────────────────────────────────────────────────────────┐
│                  Reverse Proxy / Load Balancer                                           │
│                                                                                          │
│    /                   → Next.js                                                         │
│    /api/*              → Spring Boot API                                                 │
│    /api/events/*       → Spring Boot SSE                                                 │
└───────────────┬─────────────────────────────┬─────────────────┘
│                                       │
▼                                       ▼
┌───────────────────────────────┐             ┌────────────────────────────┐
│ Next.js Frontend                       │    │ Spring Boot API                            │
│                                        │    │                                            │
│ UI și înregistrare audio               │    │ Auth, pacienți, consultații│
│ TanStack Query / HTTP client           │    │ documente, audit, upload                   │
└───────────────────────────────┘             └──────────────┬─────────────┘
│
┌───────────────────────────────┼──────────────┐
│                                       │                             │
▼                                       ▼                             ▼
┌──────────────────┐                     ┌──────────────────┐             ┌───────────┐
│ PostgreSQL             │               │ Object Storage         │       │ SSE               │
│                        │               │ privat                 │       │ Events            │
│ Date medicale        │              │ Audio temporar        │       │                   │
│ Joburi               │              │ PDF-uri opțional │            │ Status UI │
│ Audit                │              └────────┬─────────┘            └───────────┘
└────────┬─────────┘                                │
│                                       │
▼                                       ▼
┌───────────────────────────────────────────────────────┐
│                        AI Processing Worker                                 │
│                                                                             │
│   Job polling → Audio → STT → Pseudonimizare → LLM                          │
│         → Validare → Draft → Flaguri → Ștergere audio                       │
└───────────────────────┬───────────────────────────────┘
│
```

```text
┌──────────────┴───────────────┐
▼                                        ▼
┌──────────────────┐                     ┌────────────────────────┐
│ STT Provider             │             │ LLM Provider                    │
│ Transcriere              │             │ Structurare document            │
└──────────────────┘                     └────────────────────────┘
```

## 4. Componentele sistemului

### 4.1 Frontend – Next.js și React

Frontendul trebuie să fie o aplicație web responsive, utilizabilă atât de pe laptop, cât și de pe telefon.

**Responsabilități**

- autentificarea medicului;
- dashboard;
- căutarea și adăugarea pacienților;
- crearea consultațiilor;
- captarea audio prin browser;
- încărcarea fișierelor existente;
- afișarea statusului procesării;
- afișarea transcrierii;
- editarea documentului;
- aprobarea;
- copierea în clipboard;
- descărcarea PDF-ului;
- feedback.

**Recomandare de structură**

```text
src/
├── app/
│    ├── login/
│    ├── dashboard/
│    ├── patients/
│    │     └── [patientId]/
│    └── consultations/
│          └── [consultationId]/
│               ├── record/
│               └── review/
├── features/
│    ├── auth/
│    ├── patients/
│    ├── consultations/
│    ├── recording/
│    ├── documents/
```

```text
│    └── feedback/
├── components/
├── lib/
│    ├── api-client/
│    ├── audio/
│    └── validation/
└── types/
```

**Înregistrarea audio**

Pentru înregistrarea din browser se poate utiliza MediaRecorder.

Formatul preferat inițial:

```text
audio/webm; codecs=opus
```

Pentru un demo scurt, browserul poate păstra înregistrarea într-un Blob și o poate încărca după apăsarea butonului „Stop”.

Pentru pilotul cu consultații reale recomand încărcarea în bucăți sau multipart upload. Astfel, o închidere accidentală a browserului nu pierde întreaga înregistrare.

**Comunicarea cu backendul**

Frontendul comunică direct cu Spring Boot prin REST.

Next.js nu trebuie să devină un al doilea backend. API Routes sau Server Actions pot fi folosite doar pentru funcții strict legate de interfață, nu pentru implementarea domeniului medical.

### 4.2 Backend API – Spring Boot

Spring Boot este componenta centrală a sistemului.

**Responsabilități**

- autentificare și sesiuni;
- autorizare;
- izolarea clinicilor;
- gestionarea pacienților;
- gestionarea consultațiilor;
- înregistrarea consimțământului;
- emiterea URL-urilor de upload;
- crearea joburilor;
- gestionarea documentelor;
- versionarea drafturilor;
- aprobarea documentelor;
- audit;

- generarea PDF;
- trimiterea evenimentelor SSE.

Structura recomandată

```text
com.scribemed
├── identity
│    ├── api
│    ├── application
│    ├── domain
│    └── infrastructure
├── tenancy
├── patient
├── consultation
├── consent
├── audio
├── processing
├── transcription
├── document
├── template
├── audit
├── feedback
└── shared
```

În fiecare modul:

- api conține controllerele și DTO-urile;
- application conține use case-urile;
- domain conține entitățile și regulile;
- infrastructure conține repository-urile și integrările externe.

Nu este nevoie de Domain-Driven Design foarte complex. Obiectivul este ca integrarea cu AI, stocarea și controllerele HTTP să nu fie amestecate cu regulile domeniului.

### 4.3 Workerul de procesare

Workerul poate fi construit din același repository Spring Boot, dar pornit cu un profil separat:

```text
SPRING_PROFILES_ACTIVE=worker
```

API-ul și workerul pot folosi aceeași bază de cod și aceleași entități, dar pot fi instalate ca două containere diferite:

```text
scribemed-api
scribemed-worker
```

Responsabilitățile workerului

1. selectează un job disponibil;
2. îl marchează RUNNING ;
3. preia audio-ul;
4. validează sau convertește formatul;
5. trimite audio-ul către serviciul de transcriere;
6. salvează transcrierea;
7. pseudonimizează informațiile directe, unde este posibil;
8. trimite transcrierea către modelul de structurare;
9. validează răspunsul;
10. creează versiunea inițială a documentului;
11. creează marcajele pentru verificare;
12. șterge sau programează ștergerea audio-ului;
13. marchează jobul ca finalizat;
14. publică evenimentul pentru interfață.

## 5. Procesarea asincronă fără RabbitMQ

Pentru primii medici nu este necesar RabbitMQ.

Recomand un tabel PostgreSQL:

```text
processing_job
```

Workerul citește joburile folosind o tranzacție și:

```text
SELECT *
FROM processing_job
WHERE status IN ('PENDING', 'RETRY')
AND next_attempt_at <= NOW()
ORDER BY created_at
FOR UPDATE SKIP LOCKED
LIMIT 1;
```

Această abordare oferă:

- procesare asincronă;
- retry;
- mai mulți workeri;
- vizibilitate directă în baza de date;
- mai puțină infrastructură.

**Structura jobului**

```text
processing_job
- id
- tenant_id
- consultation_id
- job_type
- status
- attempt_count
- max_attempts
- next_attempt_at
- locked_at
- locked_by
- error_code
- error_message_sanitized
- created_at
- started_at
- completed_at
```

**Statusuri**

```text
PENDING
RUNNING
RETRY
SUCCEEDED
FAILED
CANCELLED
```

**Idempotency**

Pentru aceeași consultație nu trebuie create accidental două joburi identice.

Se poate adăuga o constrângere:

```text
UNIQUE(consultation_id, job_type, generation_number)
```

La creșterea volumului, acest mecanism poate fi înlocuit cu:

- RabbitMQ;
- Amazon SQS;
- Azure Service Bus;
- Kafka, doar dacă apar cerințe reale de event streaming.

## 6. Stocarea fișierelor audio

Audio-ul nu trebuie trimis mai întâi prin Spring Boot dacă fișierul este mare.

Fluxul recomandat este:

1. frontendul cere un URL temporar de upload;
2. backendul verifică utilizatorul și consultația;
3. backendul generează cheia obiectului;
4. backendul returnează URL-ul temporar;
5. browserul încarcă direct fișierul în object storage;
6. frontendul confirmă terminarea uploadului;
7. backendul verifică obiectul și creează jobul.

URL-urile presigned permit încărcarea temporară într-un bucket privat fără ca browserul să primească acces permanent sau credențiale cloud. Accesul este limitat de permisiunile și expirarea stabilite la generare.

**Cheia obiectului**

Nu se folosesc numele pacientului sau adresa de email în path.

```text
tenant/{tenantId}/consultation/{consultationId}/{randomUuid}.webm
```

**Bucketul audio**

Configurație recomandată:

- acces public complet blocat;
- criptare la stocare;
- acces permis doar API-ului și workerului;
- URL-uri cu expirare de 5–10 minute;
- limită de dimensiune;
- verificare de checksum;
- lifecycle rule pentru ștergere automată;
- fără numele pacientului în metadate.

**Politica de retenție**

Pentru pilot:

```text
Succes:
ștergere imediată sau în maximum câteva ore.
```

```text
Eroare temporară:
păstrare limitată pentru retry, de exemplu până la 24 de ore.
```

```text
Eroare definitivă:
ștergere după expirarea ferestrei de diagnostic.
```

Lifecycle rule-ul din object storage trebuie să fie o protecție suplimentară, nu singura metodă de ștergere.

## 7. Pipeline-ul AI

### 7.1 Etapa 1 – Validarea audio

Înainte de transcriere, workerul verifică:

- extensia;
- MIME type;
- semnătura reală a fișierului;
- dimensiunea;
- durata;
- codec-ul;
- checksum-ul;
- existența consultației asociate.

Opțional, fișierul poate fi normalizat cu FFmpeg într-un format acceptat de provider.

FFmpeg trebuie rulat într-un proces izolat, cu:

- timeout;
- limită de memorie;
- limită CPU;
- fără acces inutil la rețea;
- fișiere temporare șterse după procesare.

### 7.2 Etapa 2 – Transcriere

Workerul apelează o interfață internă:

```text
public interface TranscriptionProvider {
TranscriptionResult transcribe(TranscriptionRequest request);
}
```

Implementările pot fi:

```text
OpenAiTranscriptionProvider
AzureTranscriptionProvider
SelfHostedWhisperProvider
```

Restul sistemului nu trebuie să știe ce furnizor este utilizat.

**Rezultatul intern:**

```text
{
"language": "ro",
"durationSeconds": 842,
"segments": [
{
"id": "seg-1",
"startMs": 0,
"endMs": 5200,
"speaker": "UNKNOWN",
"text": "..."
}
]
}
```

Dacă providerul nu oferă separarea vorbitorilor, câmpul rămâne UNKNOWN.

### 7.3 Etapa 3 – Pseudonimizare

Înainte ca transcrierea să fie trimisă către modelul de structurare, se încearcă eliminarea identificatorilor direcți:

```text
Ion Popescu            → [PACIENT]
CNP                    → [IDENTIFICATOR]
adresă                 → [ADRESĂ]
telefon                → [TELEFON]
email                  → [EMAIL]
```

Pseudonimizarea nu transformă automat informația în date anonime. Dacă există o legătură între identificator și pacient, datele rămân date personale.

De aceea, componenta trebuie privită ca reducere a expunerii, nu ca eliminare completă a obligațiilor privind protecția datelor.

### 7.4 Etapa 4 – Generarea structurată

Modelului nu trebuie să i se solicite text HTML sau un document complet liber.

Recomand ca modelul să producă JSON după o schemă strictă:

```text
{
"chiefComplaint": {
"text": "",
"sourceSegmentIds": []
},
"historyOfPresentIllness": {
```

```text
"text": "",
"sourceSegmentIds": []
},
"medicalHistory": {
"text": "",
"sourceSegmentIds": []
},
"allergies": [
{
"name": "",
"status": "MENTIONED",
"sourceSegmentIds": []
}
],
"currentMedications": [
{
"name": "",
"dose": "",
"frequency": "",
"sourceSegmentIds": []
}
],
"clinicalExamination": {
"text": "",
"sourceSegmentIds": []
},
"assessment": {
"text": "",
"sourceSegmentIds": []
},
"plan": {
"text": "",
"sourceSegmentIds": []
}
}
```

Avantajul câmpului sourceSegmentIds este că fiecare informație importantă poate fi legată de zona transcrierii din care a fost extrasă.

Acest mecanism ajută la:

- verificarea informației;
- detectarea halucinațiilor;
- afișarea sursei în interfață;
- evaluarea modelului;
- explicarea erorilor.

### 7.5 Etapa 5 – Validarea rezultatului

Răspunsul modelului trebuie să treacă prin:

1. validare JSON;
2. validare JSON Schema;
3. validarea câmpurilor obligatorii;
4. validarea lungimii;
5. verificarea segmentelor sursă;
6. verificarea valorilor numerice;
7. verificarea medicamentelor și dozelor;
8. eliminarea instrucțiunilor sau markupului neașteptat.

Dacă rezultatul nu respectă schema:

- se poate face o singură încercare de reparare;
- apoi jobul este marcat pentru retry sau verificare manuală;
- nu se salvează un document corupt ca rezultat valid.

### 7.6 Etapa 6 – Safety flags

Sistemul generează marcaje pentru informațiile sensibile:

```text
MEDICATION_REVIEW
DOSAGE_REVIEW
ALLERGY_REVIEW
NUMERIC_VALUE_REVIEW
UNSUPPORTED_STATEMENT
POSSIBLE_NEGATION_ERROR
LOW_TRANSCRIPTION_CERTAINTY
```

În MVP, aceste marcaje pot fi parțial euristice.

Exemplu:

- orice doză este marcată pentru verificare;
- orice medicament este evidențiat;
- o informație fără sourceSegmentIds valide primește UNSUPPORTED_STATEMENT ;
- o valoare numerică absentă din transcriere este marcată;
- expresiile cu „nu”, „fără”, „neagă” primesc verificare suplimentară.

### 7.7 Etapa 7 – Salvarea draftului

Se păstrează:

- transcrierea brută;
- transcrierea pseudonimizată;
- JSON-ul structurat;
- textul redat pentru medic;

- providerul;
- modelul;
- versiunea promptului;
- versiunea șablonului;
- timpul de procesare;
- costul estimat;
- safety flags.

## 8. AI Gateway și evitarea dependenței de un furnizor

Backendul trebuie să definească două porturi:

```text
public interface TranscriptionProvider {
TranscriptionResult transcribe(TranscriptionRequest request);
}
```

```text
public interface ClinicalNoteProvider {
StructuredClinicalNote generate(ClinicalNoteRequest request);
}
```

Configurația furnizorului se face prin variabile de mediu:

```text
AI_TRANSCRIPTION_PROVIDER=openai
AI_NOTE_PROVIDER=openai
```

Avantajele sunt:

- schimbarea providerului fără modificarea domeniului;
- comparații între modele;
- fallback;
- teste cu provider fals;
- negocierea ulterioară a prețurilor;
- găzduire self-hosted dacă devine necesară.

Pentru orice API extern trebuie verificată configurația exactă de retenție a endpointului folosit. În documentația actuală OpenAI, datele API nu sunt utilizate implicit pentru antrenare, însă controalele de retenție diferă între endpointuri, iar Zero Data Retention necesită eligibilitate și configurare specifică. Endpointurile de transcriere audio au reguli diferite de endpointurile text, deci politica nu trebuie presupusă la nivel general.

În implementare:

```text
store = false
```

acolo unde endpointul oferă parametrul, iar prompturile, transcrierile și rezultatele nu trebuie trimise către servicii terțe de logging sau analytics.

## 9. Modelul de date

### 9.1 Entități principale

```text
Tenant / Clinic
└── User
└── Patient
└── Consultation
├── ConsentRecord
├── AudioAsset
├── ProcessingJob
├── Transcript
├── ClinicalDocument
│        └── DocumentVersion
│                 └── ReviewFlag
└── Feedback
```

### 9.2 Tabele

```text
tenant
```

```text
id UUID PK
name VARCHAR
status VARCHAR
created_at TIMESTAMP
```

Chiar dacă există un singur cabinet, tabela trebuie creată.

```text
app_user
```

```text
id UUID PK
tenant_id UUID FK
email VARCHAR
password_hash VARCHAR
display_name VARCHAR
role VARCHAR
status VARCHAR
last_login_at TIMESTAMP
created_at TIMESTAMP
updated_at TIMESTAMP
```

**Constrângere:**

```text
UNIQUE(tenant_id, email)
```

```text
patient
```

```text
id UUID PK
tenant_id UUID FK
internal_identifier VARCHAR
first_name VARCHAR
last_name VARCHAR
date_of_birth DATE NULL
sex VARCHAR NULL
created_by UUID FK
created_at TIMESTAMP
updated_at TIMESTAMP
archived_at TIMESTAMP NULL
```

Pentru MVP nu recomand ștergere fizică directă. Un pacient poate fi arhivat.

```text
consultation
```

```text
id UUID PK
tenant_id UUID FK
patient_id UUID FK
doctor_id UUID FK
template_id UUID FK
consultation_type VARCHAR
status VARCHAR
started_at TIMESTAMP NULL
ended_at TIMESTAMP NULL
created_at TIMESTAMP
updated_at TIMESTAMP
version BIGINT
```

version este utilizat pentru optimistic locking.

```text
consent_record
```

```text
id UUID PK
tenant_id UUID FK
consultation_id UUID FK
confirmation_type VARCHAR
statement_version VARCHAR
confirmed_by UUID FK
confirmed_at TIMESTAMP
```

Se salvează versiunea exactă a textului confirmat.

```text
audio_asset
```

```text
id UUID PK
tenant_id UUID FK
consultation_id UUID FK
object_key VARCHAR
original_filename VARCHAR NULL
content_type VARCHAR
size_bytes BIGINT
checksum VARCHAR
status VARCHAR
retention_until TIMESTAMP
deleted_at TIMESTAMP NULL
created_at TIMESTAMP
```

Nu se salvează URL-ul presigned.

```text
transcript
```

```text
id UUID PK
tenant_id UUID FK
consultation_id UUID FK
provider VARCHAR
model VARCHAR
language VARCHAR
raw_text TEXT
segments JSONB
pseudonymized_text TEXT
duration_seconds INTEGER
processing_duration_ms BIGINT
created_at TIMESTAMP
```

```text
note_template
```

```text
id UUID PK
tenant_id UUID FK NULL
name VARCHAR
specialty VARCHAR
schema_version VARCHAR
template_schema JSONB
prompt_version VARCHAR
active BOOLEAN
created_at TIMESTAMP
updated_at TIMESTAMP
```

tenant_id = NULL poate reprezenta un șablon global.

```text
knowledge_document
```

```text
id UUID PK
tenant_id UUID FK
title VARCHAR
source_institution VARCHAR
source_url TEXT NULL
published_at DATE NULL
version VARCHAR NULL
status VARCHAR
original_filename VARCHAR
checksum VARCHAR
object_key VARCHAR
created_at TIMESTAMP
```

Documentele din baza de cunoștințe sunt deținute de tenant. Același document
poate exista în tenanturi diferite, dar checksum-ul este unic în cadrul unui
tenant. Documentele globale necesită un model explicit separat și nu sunt
reprezentate prin `tenant_id = NULL`.

```text
knowledge_chunk
```

```text
id UUID PK
tenant_id UUID FK
document_id UUID FK
chunk_index INTEGER
content TEXT
page_from INTEGER NULL
page_to INTEGER NULL
section_title VARCHAR NULL
embedding VECTOR(384)
created_at TIMESTAMP
```

Tenantul chunkului trebuie să fie același cu tenantul documentului. Căutarea
semantică filtrează obligatoriu după tenant și include numai documente active.
Vectorii sunt păstrați în PostgreSQL prin extensia pgvector; nu se introduce o
bază de date vectorială separată.

```text
clinical_document
```

```text
id UUID PK
tenant_id UUID FK
consultation_id UUID FK
document_type VARCHAR
status VARCHAR
current_version_number INTEGER
created_at TIMESTAMP
updated_at TIMESTAMP
approved_at TIMESTAMP NULL
approved_by UUID NULL
```

```text
document_version
```

```text
id UUID PK
tenant_id UUID FK
document_id UUID FK
version_number INTEGER
source VARCHAR
structured_content JSONB
rendered_text TEXT
template_id UUID FK
template_version VARCHAR
prompt_version VARCHAR
ai_provider VARCHAR NULL
ai_model VARCHAR NULL
created_by UUID NULL
created_at TIMESTAMP
```

source:

```text
AI_GENERATED
DOCTOR_EDITED
REGENERATED
CORRECTION
```

Nu se suprascriu versiunile anterioare.

```text
review_flag
```

```text
id UUID PK
tenant_id UUID FK
document_version_id UUID FK
flag_type VARCHAR
severity VARCHAR
field_path VARCHAR
```

```text
message VARCHAR
source_segment_ids JSONB
resolved BOOLEAN
resolved_by UUID NULL
resolved_at TIMESTAMP NULL
```

```text
feedback
```

```text
id UUID PK
tenant_id UUID FK
consultation_id UUID FK
document_version_id UUID FK
rating VARCHAR
categories JSONB
comment TEXT
created_by UUID FK
created_at TIMESTAMP
```

```text
audit_event
```

```text
id UUID PK
tenant_id UUID FK
actor_user_id UUID NULL
event_type VARCHAR
entity_type VARCHAR
entity_id UUID
request_id VARCHAR
ip_hash VARCHAR NULL
user_agent_summary VARCHAR NULL
metadata JSONB
created_at TIMESTAMP
```

Auditul nu trebuie să conțină:

- textul consultației;
- numele complet al pacientului;
- promptul;
- fișa medicală;
- conținutul audio.

## 10. State machine

### 10.1 Consultație

```text
CREATED
│
▼
RECORDING
│
▼
AUDIO_UPLOADING
│
▼
AUDIO_READY
│
▼
TRANSCRIBING
│
▼
STRUCTURING
│
▼
REVIEW_REQUIRED
│
▼
APPROVED
```

Stări de eroare:

```text
UPLOAD_FAILED
TRANSCRIPTION_FAILED
GENERATION_FAILED
PROCESSING_FAILED
CANCELLED
```

Tranzițiile sunt controlate de backend. Frontendul nu poate seta arbitrar statusul unei consultații.

### 10.2 Audio

```text
PENDING_UPLOAD
AVAILABLE
PROCESSING
DELETION_PENDING
DELETED
FAILED
```

### 10.3 Document

```text
DRAFT
REVIEW_REQUIRED
APPROVED
SUPERSEDED
```

După aprobare, documentul devine imuabil.

Dacă medicul trebuie să îl modifice:

- se creează o nouă versiune;
- documentul anterior rămâne în istoric;
- evenimentul este auditat.

## 11. API REST recomandat

**Autentificare**

```text
POST /api/v1/auth/login
POST /api/v1/auth/logout
GET /api/v1/auth/me
```

**Pacienți**

```text
GET /api/v1/patients?query=
POST /api/v1/patients
GET /api/v1/patients/{patientId}
PATCH /api/v1/patients/{patientId}
GET /api/v1/patients/{patientId}/consultations
```

**Consultații**

```text
POST /api/v1/consultations
GET /api/v1/consultations/{consultationId}
GET /api/v1/consultations/recent
POST /api/v1/consultations/{consultationId}/consent-confirmations
```

**Audio**

```text
POST /api/v1/consultations/{consultationId}/audio/upload-session
POST /api/v1/consultations/{consultationId}/audio/upload-complete
GET /api/v1/consultations/{consultationId}/audio/status
DELETE /api/v1/consultations/{consultationId}/audio
```

Răspunsul pentru upload:

```text
{
"audioAssetId": "uuid",
"uploadUrl": "temporary-presigned-url",
"objectKey": "not-returned-or-opaque",
"expiresAt": "2026-07-31T14:00:00Z",
"requiredHeaders": {
"Content-Type": "audio/webm"
}
}
```

**Procesare**

```text
POST /api/v1/consultations/{consultationId}/processing
POST /api/v1/consultations/{consultationId}/processing/retry
GET /api/v1/consultations/{consultationId}/processing-status
GET /api/v1/consultations/{consultationId}/events
```

Ultimul endpoint folosește SSE.

**Evenimente:**

```text
audio.uploaded
processing.started
transcription.completed
document.generation.started
document.ready
processing.failed
audio.deleted
```

**Documente**

```text
GET /api/v1/consultations/{consultationId}/transcript
GET /api/v1/consultations/{consultationId}/document
PATCH /api/v1/documents/{documentId}/draft
POST /api/v1/documents/{documentId}/regenerations
```

```text
POST /api/v1/documents/{documentId}/approval
GET /api/v1/documents/{documentId}/versions
GET /api/v1/documents/{documentId}/pdf
```

Copy to Clipboard este executat în browser, nu necesită endpoint separat.

**Feedback**

```text
POST /api/v1/documents/{documentId}/feedback
```

## 12. Autentificare și autorizare

### 12.1 Sesiuni, nu JWT în localStorage

Pentru această aplicație recomand:

- sesiune opacă;
- cookie HttpOnly ;
- cookie Secure ;
- SameSite=Lax sau Strict ;
- expirare după inactivitate;
- rotația sesiunii după login;
- invalidarea la logout.

Spring Session JDBC poate păstra sesiunile în PostgreSQL.

Avantajul este evitarea stocării tokenurilor accesibile din JavaScript.

Dacă frontendul și backendul sunt servite de pe același domeniu:

```text
https://app.scribemed.ro
https://app.scribemed.ro/api
```

CORS devine mult mai simplu, iar suprafața atacului este redusă.

### 12.2 Autorizare în două niveluri

**Nivelul 1 – HTTP**

Exemplu:

```text
/api/v1/auth/**                → public
/api/v1/**                     → authenticated
```

**Nivelul 2 – Service layer**

Fiecare operațiune verifică:

- utilizatorul;
- tenantul;
- rolul;
- apartenența resursei la tenant;
- dreptul asupra pacientului sau consultației.

Spring Security permite autorizare la nivel de metodă cu @EnableMethodSecurity și verificări
```text
@PreAuthorize , utilă pentru aplicarea regulilor și în service layer, nu doar în controller.
```

Exemplu conceptual:

```text
@PreAuthorize("@access.canReadConsultation(authentication, #consultationId)")
public ConsultationDto getConsultation(UUID consultationId) {
// ...
}
```

### 12.3 Row-Level Security

În prima implementare, izolarea este aplicată obligatoriu în repository și service layer prin tenant_id.

Ca protecție suplimentară, PostgreSQL Row-Level Security poate restricționa rândurile accesibile unui tenant. PostgreSQL aplică o politică implicită de tip „default deny” atunci când RLS este activ și nu există o politică aplicabilă, dar rolurile privilegiate și proprietarul tabelului pot ocoli politicile, deci configurația trebuie testată atent.

RLS este defense-in-depth, nu înlocuiește verificările din aplicație.

## 13. Generarea PDF

PDF-ul trebuie generat server-side dintr-un șablon controlat.

**Flux:**

```text
Document aprobat
↓
Model de prezentare
↓
HTML sanitizat
↓
PDF renderer
```

```text
↓
Stream către utilizator
```

Pentru MVP, PDF-ul poate fi generat la cerere și transmis direct în răspuns.

Nu este obligatorie stocarea permanentă a PDF-ului, deoarece el poate fi regenerat din versiunea aprobată.

Trebuie păstrată versiunea șablonului folosit la generare.

Documentul poate conține:

- antetul cabinetului;
- datele pacientului;
- data consultației;
- secțiunile fișei;
- numele medicului;
- mențiunea că documentul a fost verificat;
- spațiu pentru semnătură și parafă.

## 14. Audit

Se auditează cel puțin:

```text
AUTH_LOGIN_SUCCEEDED
AUTH_LOGIN_FAILED
AUTH_LOGOUT
PATIENT_CREATED
PATIENT_VIEWED
PATIENT_UPDATED
CONSULTATION_CREATED
CONSENT_CONFIRMED
AUDIO_UPLOAD_STARTED
AUDIO_UPLOAD_COMPLETED
AUDIO_DELETED
TRANSCRIPT_VIEWED
DOCUMENT_GENERATED
DOCUMENT_EDITED
DOCUMENT_REGENERATED
DOCUMENT_APPROVED
DOCUMENT_PDF_EXPORTED
FEEDBACK_SUBMITTED
```

Auditul trebuie să fie append-only din perspectiva aplicației.

Utilizatorii obișnuiți nu trebuie să poată:

- modifica auditul;
- șterge auditul;
- vedea auditul altui tenant.

Logurile operaționale și auditul de business sunt lucruri diferite:

- auditul dovedește cine a făcut o acțiune;
- logurile ajută la diagnosticarea tehnică.

## 15. Securitate și protecția datelor

**Măsuri minime pentru pilotul cu pacienți reali**

- TLS pentru toate conexiunile;
- criptare la stocare pentru PostgreSQL și object storage;
- bucket privat;
- credențiale diferite pentru API și worker;
- secret management;
- parole hash-uite cu Argon2id sau bcrypt;
- rate limiting pentru login;
- protecție CSRF;
- validare strictă a fișierelor;
- limită de dimensiune;
- backup criptat;
- test de restaurare;
- audit;
- retenție configurată;
- fără date medicale în analytics;
- fără date medicale în mesajele de eroare;
- fără transcrieri în loguri;
- timeout și retry pentru API-urile AI;
- blocarea accesului dintre tenanturi;
- testarea autorizării pentru fiecare endpoint.

GDPR impune măsuri adecvate riscului, inclusiv pseudonimizare, criptare, confidențialitate, integritate, disponibilitate, recuperare și testarea periodică a măsurilor de securitate. Pentru prelucrări care pot produce un risc ridicat, articolul 35 prevede o evaluare a impactului înaintea prelucrării.

Înaintea pilotului cu pacienți reali, recomand:

- DPIA;
- data-flow map;
- lista subprocesatorilor;
- politica de retenție;
- plan de incident response;
- contract DPA;
- informare pentru pacient;

- verificarea temeiului legal împreună cu un specialist.

EDPB descrie DPIA ca un proces prin care organizația identifică și gestionează riscurile asupra datelor înaintea unei prelucrări susceptibile să genereze un risc ridicat.

## 16. Observabilitate

**Metrici tehnice**

```text
http_request_duration
http_error_rate
active_sessions
audio_upload_duration
audio_upload_failures
processing_queue_depth
transcription_duration
document_generation_duration
processing_retry_count
processing_failure_count
ai_provider_error_rate
database_connection_usage
```

**Metrici de produs**

```text
consultation_duration
time_until_draft_ready
doctor_review_duration
number_of_document_edits
document_change_ratio
number_of_regenerations
feedback_rating
cost_per_transcription
cost_per_generated_document
consultations_per_doctor
```

**Reguli de logging**

**Este permis:**

```text
consultationId=uuid
tenantId=uuid
jobId=uuid
provider=openai
```

```text
durationMs=1234
status=success
```

**Nu este permis:**

```text
patientName=Ion Popescu
transcript=Pacientul declară...
diagnosis=...
medication=...
```

Pentru erorile furnizorului AI, se salvează:

- codul erorii;
- request ID-ul providerului;
- statusul HTTP;
- durata;
- o descriere sanitizată.

Nu se salvează promptul complet în logurile generale.

## 17. Deployment recomandat

### 17.1 Mediul local

Docker Compose:

```text
nextjs
spring-api
spring-worker
postgres
minio
mailpit, opțional
```

MinIO simulează object storage-ul compatibil S3.

### 17.2 Mediul demo

Pentru conversații complet fictive:

```text
1 × Next.js container
1 × Spring API container
1 × Spring Worker container
1 × PostgreSQL managed
1 × Object Storage
```

### 17.3 Mediul pilot cu date reale

Toate componentele într-o singură regiune UE:

```text
Internet
│
▼
Load Balancer / WAF
│
├── Next.js container
└── Spring Boot API container
│
├── PostgreSQL managed, privat
├── Object Storage privat
├── Secret Manager
└── Worker container, privat
```

Recomandări:

- API-ul, workerul și baza de date în rețea privată;
- doar load balancerul este expus;
- baza de date nu are IP public;
- object storage-ul nu este public;
- accesul administrativ este restricționat;
- medii complet separate: dev , staging , production ;
- datele reale nu se copiază în dev ;
- backupurile sunt criptate;
- deployul este automatizat prin CI/CD.

Pentru MVP nu recomand Kubernetes.

Poți folosi:

- AWS în regiune UE;
- Azure în regiune UE;
- Scaleway;
- alt furnizor care oferă contracte, securitate și localizare adecvate.

Furnizorul trebuie ales după verificarea contractelor și a tuturor subprocesatorilor, nu doar după locația declarată a serverului.

## 18. Repository și CI/CD

**Structură recomandată**

```text
scribemed/
├── apps/
│    ├── web/
│    └── backend/
├── infrastructure/
│    ├── docker/
│    ├── terraform/
│    └── compose/
├── docs/
│    ├── architecture/
│    ├── adr/
│    ├── data-flow/
│    └── security/
└── .github/workflows/
```

Poate fi un monorepo pentru MVP.

**Pipeline**

**La fiecare pull request:**

```text
Frontend lint
Frontend tests
Backend compile
Backend unit tests
Integration tests
Database migration validation
Dependency scan
Secret scan
Container build
```

**La deploy:**

```text
Build imagini
Push registry
Rulează migrațiile
Deploy API
Deploy worker
Deploy frontend
Smoke tests
```

Migrațiile bazei de date trebuie gestionate cu:

```text
Flyway
```

sau un instrument echivalent.

## 19. Testare

### 19.1 Backend

- unit tests pentru reguli;
- integration tests cu PostgreSQL real prin Testcontainers;
- teste de autorizare;
- teste de tenant isolation;
- teste pentru state machine;
- teste de idempotency;
- teste de retry;
- teste pentru PDF;
- contract tests pentru furnizorii AI.

### 19.2 Frontend

- teste pentru flow-ul de înregistrare;
- teste pentru stările de procesare;
- teste pentru editor;
- teste pentru aprobarea documentului;
- teste end-to-end pentru flow-ul complet.

### 19.3 AI evaluation

Se construiește un set controlat de consultații simulate.

Trebuie să includă:

- nume de medicamente;
- doze;
- alergii;
- valori numerice;
- negații;
- informații contradictorii;
- zgomot;
- termeni medicali;
- consultații scurte și lungi;
- situații în care o secțiune lipsește.

Pentru fiecare test se definește un rezultat așteptat.

Nu se validează doar frumusețea textului. Se măsoară separat:

- medicamente corecte;
- doze corecte;
- alergii corecte;
- diagnostice menționate;
- numere corecte;
- negații corecte;
- informații inventate;
- informații omise.

## 20. Ordinea recomandată de implementare

Iterația 1 – Fundația
- repository;
- Docker Compose;
- Spring Boot;
- PostgreSQL;
- Next.js;
- migrații;
- autentificare;
- tenant;
- utilizator.

Iterația 2 – Pacienți și consultații
- CRUD minim pacient;
- creare consultație;
- status consultație;
- confirmarea informării;
- dashboard.

Iterația 3 – Audio
- captare browser;
- încărcare fișier;
- object storage;
- presigned upload;
- audio asset;
- finalizarea uploadului.

Iterația 4 – Procesarea
- processing job;
- worker;
- adapter STT;
- salvarea transcrierii;

- SSE;
- retry și erori.

Iterația 5 – Generarea fișei
- note template;
- adapter LLM;
- output JSON;
- validare;
- document version;
- review flags.

Iterația 6 – Review
- transcriere;
- editor;
- autosave;
- diff;
- regenerare;
- aprobare;
- audit.

Iterația 7 – Export și feedback
- copy;
- PDF;
- print;
- feedback;
- metrici de produs.

Iterația 8 – Pregătirea pilotului
- DPIA;
- retenție;
- hardening;
- backup;
- restore test;
- teste de separare a tenanturilor;
- ștergere audio;
- observabilitate;
- documentație operațională.

## 21. Decizii arhitecturale finale

**Se implementează acum**

- Next.js responsive;
- Spring Boot modular monolith;

- PostgreSQL;
- worker separat;
- coadă bazată pe PostgreSQL;
- object storage privat;
- upload direct cu URL temporar;
- SSE;
- output AI în JSON;
- documente versionate;
- aprobare obligatorie;
- audit;
- abstracție pentru furnizorii AI;
- ștergere automată audio;
- tenant_id peste tot;
- documente de cunoștințe și căutare semantică izolate pe tenant în PostgreSQL;

**Nu se implementează acum**

- microservicii;
- Kubernetes;
- RabbitMQ administrat;
- aplicație mobilă nativă;
- integrare SIUI;
- vector database;
- diagnostic automat;
- recomandări clinice;
- portal pacient;
- programări;
- billing;
- event sourcing;
- data warehouse;
- infrastructură multi-region.

## 22. Concluzie

Arhitectura recomandată este suficient de simplă pentru a fi construită de un fondator tehnic, dar evită cele mai periculoase scurtături:

- procesarea AI este asincronă;
- audio-ul este temporar;
- datele sunt separate pe tenant;
- providerii AI sunt interschimbabili;
- răspunsul AI este validat structural;
- fiecare informație poate avea referință către transcriere;
- documentele nu sunt aprobate automat;
- versiunile nu sunt suprascrise;
- auditul este separat de logging;
- MVP-ul poate evolua fără rescriere totală.

Arhitectura nu încearcă să construiască un EHR complet. Ea optimizează exact experiența care trebuie validată:

```text
înregistrare → transcriere → draft structurat → verificare medicală → aprobare → export.
```

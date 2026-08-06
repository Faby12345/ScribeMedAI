---
target: patient management area
total_score: 20
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 2
timestamp: 2026-08-04T11-40-47Z
slug: frontend-src-app-patients-page-tsx
---
Method: dual-agent (A: 019fcc90-4967-7c93-af46-4ae17195d08b · B: 019fcc90-6dcf-7360-ba18-bc4a35363baf)

## Design Health Score

| # | Heuristic | Score | Key Issue |
|---|-----------|-------|-----------|
| 1 | Visibility of System Status | 2 | Loading/error states exist, but unavailable consultation history and backend-gap copy expose incompleteness instead of useful patient workflow status. |
| 2 | Match System / Real World | 2 | Mostly Romanian and clinically calm, but `Overview`, `endpoint`, and `backend` break doctor-facing language. |
| 3 | User Control and Freedom | 2 | Back/cancel paths exist, but the edit route is a dead-end and the patient list only offers `Deschide`, not the likely consultation-start action. |
| 4 | Consistency and Standards | 2 | Shared primitives are strong, but link-buttons repeat long class strings and the patient list/picker use different action models. |
| 5 | Error Prevention | 2 | Create form validation and privacy warnings exist, but duplicate-patient prevention and wrong-patient safeguards are thin. |
| 6 | Recognition Rather Than Recall | 2 | Desktop table labels help, but mobile rows lose labels and surface IDs/internal absence states more than human patient cues. |
| 7 | Flexibility and Efficiency | 2 | Search is client-side over the loaded list, there is no count/filtering/pagination, and no direct consultation start from the main list. |
| 8 | Aesthetic and Minimalist Design | 3 | Restrained medical SaaS foundation works, but the area still feels like a generic admin registry rather than a consultation launch surface. |
| 9 | Error Recovery | 2 | Retry and submit errors exist, but recovery copy is generic and unavailable flows do not offer a practical next step. |
| 10 | Help and Documentation | 1 | Helper copy often explains implementation limitations instead of guiding doctors through patient work. |
| **Total** | | **20/40** | **Functional baseline, but not yet an efficient medical Operate surface.** |

## Design Specificity Verdict

The patient management area is competent but category-interchangeable. It looks like a clean SaaS admin registry that could belong to many products. For ScribeMedAI, the patient area should feel like the beginning of the consultation workflow: find the right patient, confirm identity confidently, and start documentation work quickly.

**LLM assessment**: The interface has calm medical SaaS tokens and a reusable component foundation, but its interaction model is still record-browser first. The most product-specific action, `Începe consultația`, appears only after entering a profile, while the main list’s row action is the generic `Deschide`.

**Deterministic scan**: The detector returned `[]` with exit code `0` for `frontend/src/app/patients` and `frontend/src/features/patients`. No rule findings or file locations were reported. This confirms there were no bundled detector violations, but it does not clear the higher-level UX issues.

**Visual overlays**: No reliable user-visible overlay is available. Browser automation/tab tools are not exposed in this session, so no mutable injection or `[Human]` overlay could be attempted.

## Overall Impression

The foundation is sober, accessible, and aligned with a medical SaaS MVP, but the patient area is too honest about implementation state and not decisive enough about the doctor’s real job. The biggest opportunity is to redesign the list around consultation readiness instead of passive profile browsing.

## What's Working

The token system is restrained and appropriate: background, surface, border, primary, success, warning, destructive, focus ring, and shadow tokens are centralized in `frontend/src/app/globals.css`.

The create patient form is properly minimal for an MVP that values data minimization: only name fields are required, optional contact/demographic fields are clearly labeled, and loading/error states are present.

The patient profile page correctly makes `Începe consultația` the primary action once the doctor reaches the profile.

## Priority Issues

**[P1] Doctor-facing copy exposes implementation gaps**

Why it matters: Doctors managing sensitive patient data should not see text about endpoints, backend availability, or future implementation. It lowers trust and makes the product feel like a prototype instead of a clinical tool.

Fix: Replace implementation notes with user-centered, calm unavailable states or remove them when they do not guide action. Use copy such as `Căutarea se aplică pacienților afișați` only if needed, `Istoricul consultațiilor nu este disponibil momentan`, and `Editarea va fi disponibilă după activarea fluxului de actualizare` only where the route must remain accessible.

Suggested command: `$impeccable clarify`

**[P1] The main patient list is not optimized for starting consultations**

Why it matters: The MVP workflow starts with selecting a patient and creating a consultation. The current list requires find patient -> open profile -> start consultation, adding friction to the primary daily path.

Fix: Add row-level actions with clear hierarchy: `Începe consultația` as the primary action where clinically valid, and `Deschide profilul` as secondary. If the whole row remains clickable, make nested actions unambiguous and keyboard-safe.

Suggested command: `$impeccable layout`

**[P2] Information hierarchy favors admin metadata over clinical usefulness**

Why it matters: Patient IDs, created/updated timestamps, and `Indisponibilă` occupy attention without helping the doctor choose the right patient quickly. Wrong-patient prevention needs recognizable patient cues, not internal metadata.

Fix: Prioritize full name, birth date/age, phone or email, active/archived status, and last consultation status/date when available. Move IDs and technical timestamps into subdued detail areas or copy-only affordances.

Suggested command: `$impeccable distill`

**[P2] Navigation and route semantics are not fully Romanian/product-specific**

Why it matters: `Overview` is an English leak in a Romanian medical product. `Setări` routing to `/dashboard` also makes navigation feel unfinished.

Fix: Translate or rename `Overview`, make shell labels match the actual application scope, and avoid route labels that promise unavailable sections.

Suggested command: `$impeccable clarify`

**[P3] Shared components exist, but link-style buttons bypass the component system**

Why it matters: Long repeated class strings make the interface harder to keep consistent and increase regression risk when token or focus styles change.

Fix: Add an anchor-capable button pattern, or a shared `ButtonLink` wrapper, then replace repeated link-button class strings in patient pages and list empty states.

Suggested command: `$impeccable extract`

## Persona Red Flags

**Dr. Andrei, time-pressed clinician**: He can search patients, but the visible row action says `Deschide`. The workflow makes him open the profile before the actual primary action, even when his intent is immediately starting a consultation.

**Dr. Ioana, first-time user**: She sees `endpoint`, `backend`, and `Editare indisponibilă`. That language makes the tool feel unfinished and less dependable in a clinical context.

**Clinic power user**: Search is only local to the loaded list, with no visible count, pagination, server filtering, or quick recent-patient affordance. A larger clinic registry will make result confidence poor.

## Minor Observations

Mobile patient rows need visible field labels because the desktop header disappears.

The edit page should not be a prominent profile action until editing works, unless it is explicitly framed as read-only details.

The profile alert about not entering medical data in contact fields is useful, but it would be stronger near the relevant input fields during create/edit rather than only after the profile exists.

`Ultima consultație: Indisponibilă` should be hidden until real data exists or reframed as a neutral empty state.

## Questions to Consider

- What if the patient list’s primary action were `Începe consultația`, with profile viewing secondary?
- Should doctors ever see MVP implementation limitations, or should incomplete features always become calm unavailable states?
- Which patient details best prevent choosing the wrong person in a Romanian clinic: phone, birth date, age, last visit, or another identifier?

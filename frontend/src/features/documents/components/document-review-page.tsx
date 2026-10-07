"use client";

import { type KeyboardEvent as ReactKeyboardEvent, useMemo, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button, ButtonLink } from "@/components/ui/button";
import { Checkbox, Textarea } from "@/components/ui/input";
import {
  ApproveDocumentApiError,
  approveDocument,
} from "@/features/documents/api/approve-document";
import {
  SaveDocumentDraftApiError,
  saveDocumentDraft,
} from "@/features/documents/api/save-document-draft";
import type {
  DocumentReviewDocument,
  SoapDraft,
  SoapSectionId,
} from "@/features/documents/types";
import { cn } from "@/lib/class-names";

type ReviewFlag = {
  id: string;
  label: string;
  description: string;
  severity: "warning" | "destructive";
  sectionId: SoapSectionId;
};

type SaveState = "idle" | "dirty" | "saved";
type OperationState = "idle" | "saving" | "approving";
type ReviewSectionId = SoapSectionId | "medications";

type StatusMessage = {
  variant: "info" | "error" | "success" | "warning";
  title: string;
  text: string;
};

type DocumentReviewPageProps = {
  consultationId: string;
  patientBirthDate?: string | null;
  patientName?: string | null;
  reviewDocument?: DocumentReviewDocument | null;
};

const sectionLabels: Record<SoapSectionId, string> = {
  subjective: "Subiectiv",
  objective: "Obiectiv",
  assessment: "Evaluare",
  plan: "Plan",
};

const reviewSections: ReviewSectionId[] = [
  "subjective",
  "objective",
  "assessment",
  "plan",
  "medications",
];

const emptyDraft: SoapDraft = {
  subjective: "",
  objective: "",
  assessment: "",
  plan: "",
};

const initialReviewedSections: Record<SoapSectionId, boolean> = {
  subjective: false,
  objective: false,
  assessment: false,
  plan: false,
};

const mockDraft: SoapDraft = {
  subjective:
    "Pacientul se prezintă pentru tuse seacă, rinoree și subfebrilitate apărute de aproximativ 3 zile. Neagă dispnee și durere toracică. A administrat paracetamol ocazional, cu ameliorare parțială.",
  objective:
    "Stare generală bună, afebril la momentul consultului. Faringe discret hiperemic. Auscultație pulmonară fără raluri evidente. Saturație raportată 98%.",
  assessment:
    "Tablou clinic sugestiv pentru infecție respiratorie acută de căi superioare. Informațiile provin din transcriere și necesită verificarea medicului înainte de aprobare.",
  plan:
    "Tratament simptomatic, hidratare, monitorizarea temperaturii. Revenire la consult dacă apar dispnee, febră persistentă sau agravarea simptomelor. Dozele medicamentoase trebuie confirmate de medic.",
};

const mockTranscript = `Medic: Ce vă supără cel mai mult?
Pacient: De trei zile tușesc sec și îmi curge nasul. Am avut și o temperatură mică.
Medic: Aveți lipsă de aer sau durere în piept?
Pacient: Nu, nu am avut așa ceva.
Medic: Ați luat ceva până acum?
Pacient: Paracetamol, din când în când. M-a ajutat puțin.
Medic: Alergii la medicamente?
Pacient: Nu știu să am alergii.`;

const mockFlags: ReviewFlag[] = [
  {
    id: "dosage-review",
    label: "Doză medicamentoasă",
    description:
      "Paracetamol este menționat fără doză sau frecvență completă.",
    severity: "warning",
    sectionId: "plan",
  },
  {
    id: "numeric-value-review",
    label: "Valoare numerică",
    description: "Saturația 98% trebuie confirmată în contextul sursei.",
    severity: "warning",
    sectionId: "objective",
  },
  {
    id: "negation-review",
    label: "Negație clinică",
    description:
      "Absența dispneei și a durerii toracice trebuie verificată explicit.",
    severity: "destructive",
    sectionId: "subjective",
  },
];

export function DocumentReviewPage({
  consultationId,
  patientBirthDate,
  patientName,
  reviewDocument,
}: DocumentReviewPageProps) {
  const [currentDocument, setCurrentDocument] =
    useState<DocumentReviewDocument | null>(reviewDocument ?? null);
  const [draft, setDraft] = useState<SoapDraft>(
    reviewDocument?.draft ?? emptyDraft,
  );
  const [transcript, setTranscript] = useState(
    reviewDocument?.transcript?.transcriptText ?? "",
  );
  const [reviewFlags, setReviewFlags] = useState<ReviewFlag[]>(
    () => reviewDocument?.reviewFlags.map(toReviewFlag) ?? [],
  );
  const [reviewedSections, setReviewedSections] = useState(
    initialReviewedSections,
  );
  const [resolvedFlags, setResolvedFlags] = useState<Record<string, boolean>>(
    {},
  );
  const [saveState, setSaveState] = useState<SaveState>(
    reviewDocument ? "saved" : "idle",
  );
  const [operationState, setOperationState] =
    useState<OperationState>("idle");
  const [statusMessage, setStatusMessage] = useState<StatusMessage | null>(
    null,
  );
  const [isDemoDraft, setIsDemoDraft] = useState(false);
  const [activeSection, setActiveSection] =
    useState<ReviewSectionId>("subjective");

  const reviewedSectionCount = Object.values(reviewedSections).filter(Boolean)
    .length;
  const resolvedFlagCount = reviewFlags.filter((flag) => resolvedFlags[flag.id])
    .length;
  const hasDraftContent = Object.values(draft).some((value) => value.trim());
  const allSectionsReviewed = reviewedSectionCount === 4;
  const allFlagsResolved =
    reviewFlags.length === 0 || resolvedFlagCount === reviewFlags.length;
  const isApproved = currentDocument?.documentStatus === "APPROVED";
  const isDoctorCreated = Boolean(currentDocument && !currentDocument.aiProvider);
  const isSaving = operationState === "saving";
  const isApproving = operationState === "approving";
  const isBusy = operationState !== "idle";
  const canApprove =
    Boolean(currentDocument) &&
    !isApproved &&
    !isDemoDraft &&
    !isBusy &&
    hasDraftContent &&
    allSectionsReviewed &&
    allFlagsResolved &&
    saveState === "saved";

  const documentStatus = useMemo(() => {
    if (!hasDraftContent) {
      return { label: "Draft neîncărcat", variant: "neutral" as const };
    }

    if (isApproved) {
      return { label: "Aprobat", variant: "success" as const };
    }

    if (canApprove) {
      return { label: "Gata de aprobare", variant: "success" as const };
    }

    if (saveState === "dirty") {
      return { label: "Modificări nesalvate", variant: "warning" as const };
    }

    return { label: "În revizuire", variant: "processing" as const };
  }, [canApprove, hasDraftContent, isApproved, saveState]);

  function fillMockData() {
    setDraft(mockDraft);
    setTranscript(mockTranscript);
    setReviewFlags(mockFlags);
    setReviewedSections(initialReviewedSections);
    setResolvedFlags({});
    setSaveState("dirty");
    setIsDemoDraft(true);
    setStatusMessage(null);
  }

  function updateSection(sectionId: SoapSectionId, value: string) {
    if (isApproved) {
      return;
    }

    setDraft((current) => ({ ...current, [sectionId]: value }));
    setReviewedSections((current) => ({ ...current, [sectionId]: false }));
    setSaveState("dirty");
    setIsDemoDraft(false);
    setStatusMessage(null);
  }

  function toggleSectionReview(sectionId: SoapSectionId, checked: boolean) {
    if (isApproved) {
      return;
    }

    setReviewedSections((current) => ({ ...current, [sectionId]: checked }));
    setStatusMessage(null);
  }

  function toggleFlag(flagId: string, checked: boolean) {
    if (isApproved) {
      return;
    }

    setResolvedFlags((current) => ({ ...current, [flagId]: checked }));
    setStatusMessage(null);
  }

  async function handleSaveDraft() {
    if (!hasDraftContent) {
      return;
    }

    if (isDemoDraft) {
      setStatusMessage({
        variant: "warning",
        title: "Date demonstrative",
        text: "Draftul demonstrativ nu poate fi salvat peste documentul real.",
      });
      return;
    }

    if (!currentDocument) {
      setStatusMessage({
        variant: "warning",
        title: "Draft indisponibil",
        text: "Încarcă un document real înainte de salvare.",
      });
      return;
    }

    setOperationState("saving");
    setStatusMessage(null);

    try {
      const unresolvedReviewFlags = reviewFlags
        .filter((flag) => !resolvedFlags[flag.id])
        .map((flag) => flag.description);
      const savedDocument = await saveDocumentDraft({
        documentId: currentDocument.documentId,
        draft,
        reviewFlags: unresolvedReviewFlags,
      });

      setCurrentDocument(savedDocument);
      setDraft(savedDocument.draft);
      setTranscript(savedDocument.transcript?.transcriptText ?? "");
      setReviewFlags(savedDocument.reviewFlags.map(toReviewFlag));
      setResolvedFlags({});
      setSaveState("saved");
      setIsDemoDraft(false);
      setStatusMessage({
        variant: "success",
        title: "Draft salvat",
        text: "Modificările au fost salvate ca versiune nouă de draft.",
      });
    } catch (error) {
      setStatusMessage({
        variant: "error",
        title: "Salvare eșuată",
        text:
          error instanceof SaveDocumentDraftApiError
            ? error.message
            : "Draftul nu a putut fi salvat. Încearcă din nou.",
      });
    } finally {
      setOperationState("idle");
    }
  }

  async function handleApproveDraft() {
    if (!canApprove) {
      return;
    }

    if (!currentDocument) {
      return;
    }

    setOperationState("approving");
    setStatusMessage(null);

    try {
      const approval = await approveDocument(
        currentDocument.documentId,
        currentDocument.versionId,
      );

      setCurrentDocument({
        ...currentDocument,
        documentStatus: approval.documentStatus,
        versionStatus: approval.versionStatus,
        versionId: approval.versionId,
        versionNumber: approval.versionNumber,
      });
      setSaveState("saved");
      setStatusMessage({
        variant: "success",
        title: "Document aprobat",
        text: "Documentul clinic a fost aprobat și nu mai poate fi editat din acest flux.",
      });
    } catch (error) {
      setStatusMessage({
        variant: "error",
        title: "Aprobare eșuată",
        text:
          error instanceof ApproveDocumentApiError
            ? error.message
            : "Documentul nu a putut fi aprobat. Încearcă din nou.",
      });
    } finally {
      setOperationState("idle");
    }
  }

  return (
    <div className="mx-auto w-full max-w-[92rem] px-5 py-6 sm:px-8 sm:py-8 lg:px-10">
      <header className="border-b border-border pb-5">
        <ButtonLink
          href={`/consultations/${consultationId}`}
          variant="ghost"
          size="sm"
          className="-ml-3 mb-3"
        >
          <BackIcon />
          Înapoi la consultație
        </ButtonLink>
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0">
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <Badge variant={documentStatus.variant}>
                {documentStatus.label}
              </Badge>
              <Badge variant="neutral">SOAP</Badge>
              {patientName ? (
                <span className="text-sm font-semibold text-foreground">
                  {patientName}
                </span>
              ) : null}
              {patientBirthDate ? (
                <span className="caption-text">
                  Născut(ă) la {formatDate(patientBirthDate)}
                </span>
              ) : null}
              <span className="caption-text">
                Consultație {shortId(consultationId)}
              </span>
            </div>
            <h1 className="page-title">Revizuire document clinic</h1>
            <p className="secondary-text mt-2 max-w-3xl">
              {isDoctorCreated
                ? "Draft creat din notițele medicului, pregătit pentru verificare și aprobare."
                : "Draft generat AI, pregătit pentru corectură medicală și aprobare."}
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button type="button" variant="ghost" onClick={fillMockData}>
              Completează demo
            </Button>
          </div>
        </div>
      </header>

      <ReviewCommandBar
        reviewedSectionCount={reviewedSectionCount}
        resolvedFlagCount={resolvedFlagCount}
        flagCount={reviewFlags.length}
        saveState={saveState}
        canApprove={canApprove}
        hasDraftContent={hasDraftContent}
        isApproved={isApproved}
        isSaving={isSaving}
        isApproving={isApproving}
        onSave={handleSaveDraft}
        onApprove={handleApproveDraft}
      />

      {statusMessage ? (
        <div className="mt-5 max-w-3xl">
          <Alert variant={statusMessage.variant} title={statusMessage.title}>
            {statusMessage.text}
          </Alert>
        </div>
      ) : null}

      <div className="mt-7 grid min-w-0 gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(19rem,27rem)] xl:gap-10">
        <main className="min-w-0">
          <section className="overflow-hidden rounded-[var(--radius-surface)] bg-surface shadow-surface">
            <div className="border-b border-border px-5 py-5 sm:px-7">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-semibold text-foreground">
                    Document clinic
                  </h2>
                  <p className="caption-text mt-1">
                    {isDoctorCreated
                      ? "Versiune creată de medic, neaprobată"
                      : "Versiune generată AI, neaprobată"}
                  </p>
                </div>
                <Badge variant={saveState === "saved" ? "success" : "warning"}>
                  {saveState === "saved" ? "Salvat" : "Nesalvat"}
                </Badge>
              </div>
            </div>

            <ReviewSectionTabs
              activeSection={activeSection}
              reviewedSections={reviewedSections}
              medicationCount={currentDocument?.medications.length ?? 0}
              onChange={setActiveSection}
            />

            <div id="review-section-panel" role="tabpanel" aria-labelledby={`review-tab-${activeSection}`}>
              {activeSection === "medications" ? (
                <MedicationReviewPanel
                  medications={currentDocument?.medications ?? []}
                />
              ) : (
                <SoapSectionEditor
                  sectionId={activeSection}
                  label={sectionLabels[activeSection]}
                  value={draft[activeSection]}
                  reviewed={reviewedSections[activeSection]}
                  disabled={isApproved || isBusy}
                  onChange={(value) => updateSection(activeSection, value)}
                  onReviewedChange={(checked) =>
                    toggleSectionReview(activeSection, checked)
                  }
                />
              )}
            </div>
          </section>
        </main>

        <aside className="min-w-0 space-y-8 lg:sticky lg:top-40 lg:max-h-[calc(100dvh-11rem)] lg:self-start lg:overflow-y-auto lg:pr-2">
          <TranscriptPanel transcript={transcript} />
          <ReviewFlagsPanel
            flags={reviewFlags}
            resolvedFlags={resolvedFlags}
            disabled={isApproved || isBusy}
            onResolvedChange={toggleFlag}
          />
        </aside>
      </div>
    </div>
  );
}

function ReviewSectionTabs({
  activeSection,
  reviewedSections,
  medicationCount,
  onChange,
}: {
  activeSection: ReviewSectionId;
  reviewedSections: Record<SoapSectionId, boolean>;
  medicationCount: number;
  onChange: (section: ReviewSectionId) => void;
}) {
  function handleKeyDown(
    event: ReactKeyboardEvent<HTMLButtonElement>,
    section: ReviewSectionId,
  ) {
    const currentIndex = reviewSections.indexOf(section);
    let nextIndex: number | null = null;

    if (event.key === "ArrowRight") {
      nextIndex = (currentIndex + 1) % reviewSections.length;
    } else if (event.key === "ArrowLeft") {
      nextIndex = (currentIndex - 1 + reviewSections.length) % reviewSections.length;
    } else if (event.key === "Home") {
      nextIndex = 0;
    } else if (event.key === "End") {
      nextIndex = reviewSections.length - 1;
    }

    if (nextIndex === null) {
      return;
    }

    event.preventDefault();
    const nextSection = reviewSections[nextIndex];
    onChange(nextSection);
    document.getElementById(`review-tab-${nextSection}`)?.focus();
  }

  return (
    <div
      className="overflow-x-auto border-b border-border px-5 sm:px-7"
      role="tablist"
      aria-label="Secțiunile documentului clinic"
    >
      <div className="flex min-w-max gap-6">
        {reviewSections.map((section) => {
          const isMedication = section === "medications";
          const isActive = activeSection === section;
          const isReviewed = !isMedication && reviewedSections[section];
          const label = isMedication ? "Tratament" : sectionLabels[section];

          return (
            <button
              key={section}
              id={`review-tab-${section}`}
              type="button"
              role="tab"
              aria-selected={isActive}
              aria-controls="review-section-panel"
              tabIndex={isActive ? 0 : -1}
              className={cn(
                "relative flex min-h-12 items-center gap-2 whitespace-nowrap py-3 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2",
                isActive
                  ? "text-primary after:absolute after:inset-x-0 after:bottom-0 after:h-0.5 after:bg-primary"
                  : "text-muted-foreground hover:text-foreground",
              )}
              onClick={() => onChange(section)}
              onKeyDown={(event) => handleKeyDown(event, section)}
            >
              {!isMedication ? (
                <span
                  className={cn(
                    "size-2 rounded-full",
                    isReviewed ? "bg-success" : "bg-warning",
                  )}
                  aria-hidden="true"
                />
              ) : null}
              {label}
              {isMedication ? (
                <span className="text-xs text-muted-foreground">
                  ({medicationCount})
                </span>
              ) : null}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function MedicationReviewPanel({
  medications,
}: {
  medications: DocumentReviewDocument["medications"];
}) {
  return (
    <section
      aria-labelledby="medication-review-title"
      className="min-h-[28rem]"
    >
      <div className="flex flex-col gap-2 border-b border-border px-5 py-5 sm:flex-row sm:items-center sm:justify-between sm:px-7">
        <div>
          <h2 id="medication-review-title" className="text-sm font-semibold text-foreground">
            Tratament medicamentos
          </h2>
          <p className="caption-text mt-1">
            Schema introdusă de medic în notițele consultației
          </p>
        </div>
        <Badge variant={medications.length > 0 ? "processing" : "neutral"}>
          {medications.length} {medications.length === 1 ? "medicament" : "medicamente"}
        </Badge>
      </div>

      {medications.length === 0 ? (
        <p className="mx-5 my-6 border-y border-dashed border-border py-5 text-sm text-muted-foreground sm:mx-7">
          Nu a fost adăugat niciun medicament pentru această consultație.
        </p>
      ) : (
        <ol className="max-h-[28rem] divide-y divide-border overflow-y-auto">
          {medications.map((medication, index) => (
            <li key={medication.id} className="px-5 py-6 sm:px-7">
              <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <h3 className="text-base font-semibold text-foreground">
                    {index + 1}. {medication.commercialName ?? "Denumire indisponibilă"}
                  </h3>
                  <p className="mt-2 text-sm font-medium text-primary">
                    {medication.dose} · {medication.administrationRoute} · {medication.frequency} · {medication.duration}
                  </p>
                </div>
                <Badge variant="neutral">CIM {medication.cimCode}</Badge>
              </div>

              <dl className="mt-5 grid gap-x-6 gap-y-4 border-t border-border pt-5 sm:grid-cols-2 xl:grid-cols-3">
                <MedicationDetail
                  label="Substanță activă"
                  value={medication.activeSubstance}
                />
                <MedicationDetail
                  label="Formă farmaceutică"
                  value={medication.pharmaceuticalForm}
                />
                <MedicationDetail
                  label="Concentrație"
                  value={medication.concentration}
                />
                <MedicationDetail
                  label="Tip prescripție"
                  value={medication.prescriptionType}
                />
                <MedicationDetail label="Cod CIM" value={medication.cimCode} />
                <MedicationDetail label="Doză" value={medication.dose} />
                <MedicationDetail
                  label="Cale de administrare"
                  value={medication.administrationRoute}
                />
                <MedicationDetail
                  label="Frecvență"
                  value={medication.frequency}
                />
                <MedicationDetail label="Durată" value={medication.duration} />
                {medication.quantity ? (
                  <MedicationDetail label="Cantitate" value={medication.quantity} />
                ) : null}
              </dl>

              {medication.instructions || medication.notes ? (
                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  {medication.instructions ? (
                    <MedicationTextDetail
                      label="Instrucțiuni"
                      value={medication.instructions}
                    />
                  ) : null}
                  {medication.notes ? (
                    <MedicationTextDetail
                      label="Observații"
                      value={medication.notes}
                    />
                  ) : null}
                </div>
              ) : null}
            </li>
          ))}
        </ol>
      )}
    </section>
  );
}

function MedicationDetail({
  label,
  value,
}: {
  label: string;
  value: string | null;
}) {
  return (
    <div className="min-w-0">
      <dt className="caption-text">{label}</dt>
      <dd className="mt-1 text-sm font-medium text-foreground">
        {value ?? "Nespecificat"}
      </dd>
    </div>
  );
}

function MedicationTextDetail({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-[var(--radius-control)] bg-surface-muted px-4 py-3">
      <p className="caption-text">{label}</p>
      <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-foreground">
        {value}
      </p>
    </div>
  );
}

function ReviewCommandBar({
  reviewedSectionCount,
  resolvedFlagCount,
  flagCount,
  saveState,
  canApprove,
  hasDraftContent,
  isApproved,
  isSaving,
  isApproving,
  onSave,
  onApprove,
}: {
  reviewedSectionCount: number;
  resolvedFlagCount: number;
  flagCount: number;
  saveState: SaveState;
  canApprove: boolean;
  hasDraftContent: boolean;
  isApproved: boolean;
  isSaving: boolean;
  isApproving: boolean;
  onSave: () => void;
  onApprove: () => void;
}) {
  return (
    <div className="sticky top-[4.25rem] z-20 -mx-5 border-b border-border bg-background px-5 py-3 sm:-mx-8 sm:px-8 lg:-mx-10 lg:px-10" aria-live="polite">
      <div className="mx-auto flex max-w-[92rem] flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div className="flex min-w-0 flex-wrap items-center gap-x-4 gap-y-2 text-sm">
          <ReviewProgressItem
            label="Secțiuni"
            value={`${reviewedSectionCount}/4`}
            complete={reviewedSectionCount === 4}
          />
          <ReviewProgressItem
            label="Marcaje"
            value={`${resolvedFlagCount}/${flagCount}`}
            complete={flagCount === 0 || resolvedFlagCount === flagCount}
          />
          <ReviewProgressItem
            label="Salvare"
            value={saveState === "saved" ? "Salvat" : "Nesalvat"}
            complete={saveState === "saved"}
          />
        </div>
        <div className="flex w-full gap-2 sm:w-auto">
          <Button
            type="button"
            variant={saveState === "dirty" ? "primary" : "outline"}
            className="flex-1 sm:flex-none"
            onClick={onSave}
            disabled={
              isApproved || isApproving || !hasDraftContent || saveState === "saved"
            }
            isLoading={isSaving}
            loadingText="Se salvează"
          >
            {saveState === "saved" ? "Draft salvat" : "Salvează draftul"}
          </Button>
          <Button
            type="button"
            variant="success"
            className="flex-1 sm:flex-none"
            onClick={onApprove}
            disabled={isApproved || !canApprove}
            isLoading={isApproving}
            loadingText="Se aprobă"
          >
            {isApproved ? "Document aprobat" : "Aprobă documentul"}
          </Button>
        </div>
      </div>
    </div>
  );
}

function ReviewProgressItem({
  label,
  value,
  complete,
}: {
  label: string;
  value: string;
  complete: boolean;
}) {
  return (
    <span className="inline-flex items-center gap-2 whitespace-nowrap">
      <span
        className={cn("size-2 rounded-full", complete ? "bg-success" : "bg-warning")}
        aria-hidden="true"
      />
      <span className="text-muted-foreground">{label}</span>
      <span className="font-semibold text-foreground">{value}</span>
    </span>
  );
}

function SoapSectionEditor({
  sectionId,
  label,
  value,
  reviewed,
  disabled,
  onChange,
  onReviewedChange,
}: {
  sectionId: SoapSectionId;
  label: string;
  value: string;
  reviewed: boolean;
  disabled: boolean;
  onChange: (value: string) => void;
  onReviewedChange: (checked: boolean) => void;
}) {
  const textareaId = `soap-${sectionId}`;

  return (
    <section className="grid min-h-[28rem] gap-4 px-5 py-6 sm:px-7 xl:grid-cols-[9rem_minmax(0,1fr)] xl:gap-6">
      <div className="min-w-0">
        <label
          className="block text-sm font-semibold text-foreground"
          htmlFor={textareaId}
        >
          {label}
        </label>
        <div className="mt-2">
          <Badge variant={reviewed ? "success" : "warning"}>
            {reviewed ? "Revizuit" : "De verificat"}
          </Badge>
        </div>
      </div>

      <div className="min-w-0">
        <Textarea
          id={textareaId}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder="Text draft"
          disabled={disabled}
          className="min-h-72 border-input bg-surface-muted/60 text-base leading-7 shadow-none focus:bg-surface"
        />
        <div className="mt-3 flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <Checkbox
            label={`Secțiunea ${label} este verificată`}
            checked={reviewed}
            onChange={(event) => onReviewedChange(event.target.checked)}
            disabled={disabled || !value.trim()}
          />
          <span className="caption-text sm:text-right">
            Editarea resetează verificarea.
          </span>
        </div>
      </div>
    </section>
  );
}

function ReviewFlagsPanel({
  flags,
  resolvedFlags,
  disabled,
  onResolvedChange,
}: {
  flags: ReviewFlag[];
  resolvedFlags: Record<string, boolean>;
  disabled: boolean;
  onResolvedChange: (flagId: string, checked: boolean) => void;
}) {
  return (
    <section aria-labelledby="review-flags-title" className="border-t border-border pt-5">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 id="review-flags-title" className="section-title">Marcaje clinice</h2>
          <p className="caption-text mt-1">Verifică punctele semnalate înainte de aprobare.</p>
        </div>
        <Badge variant={flags.length ? "warning" : "neutral"}>{flags.length}</Badge>
      </div>
      <div className="mt-4">
        {flags.length === 0 ? (
          <p className="border-y border-dashed border-border py-4 text-sm text-muted-foreground">
            Niciun marcaj încă.
          </p>
        ) : (
          <div className="divide-y divide-border border-y border-border">
            {flags.map((flag) => (
              <div
                key={flag.id}
                className="py-4"
              >
                <div className="mb-2 flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-foreground">
                      {flag.label}
                    </p>
                    <p className="caption-text mt-0.5">
                      {sectionLabels[flag.sectionId]}
                    </p>
                  </div>
                  <Badge
                    variant={
                      flag.severity === "destructive"
                        ? "destructive"
                        : "warning"
                    }
                  >
                    {flag.severity === "destructive" ? "Ridicat" : "Atenție"}
                  </Badge>
                </div>
                <p className="mb-3 text-sm leading-6 text-muted-foreground">
                  {flag.description}
                </p>
                <Checkbox
                  label="Verificat"
                  checked={Boolean(resolvedFlags[flag.id])}
                  disabled={disabled}
                  onChange={(event) =>
                    onResolvedChange(flag.id, event.target.checked)
                  }
                />
              </div>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function TranscriptPanel({ transcript }: { transcript: string }) {
  return (
    <section aria-labelledby="transcript-title">
      <div className="flex items-end justify-between gap-4 border-b border-border pb-3">
        <div>
          <h2 id="transcript-title" className="section-title">Transcriere</h2>
          <p className="caption-text mt-1">Referință pentru verificarea documentului</p>
        </div>
      </div>
      <div className="pt-4">
        {transcript ? (
          <div className="max-h-[50vh] overflow-y-auto bg-surface-muted px-4 py-4">
            <pre className="whitespace-pre-wrap font-sans text-sm leading-6 text-foreground">
              {transcript}
            </pre>
          </div>
        ) : (
          <p className="border-y border-dashed border-border py-4 text-sm text-muted-foreground">
            Nicio transcriere încă.
          </p>
        )}
      </div>
    </section>
  );
}

function BackIcon() {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="m15 18-6-6 6-6" /></svg>;
}

function shortId(value: string) {
  return value.length > 8 ? value.slice(0, 8) : value;
}

function formatDate(value: string) {
  const date = new Date(`${value}T00:00:00`);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("ro-RO", {
    day: "2-digit",
    month: "long",
    year: "numeric",
  }).format(date);
}

function toReviewFlag(description: string, index: number): ReviewFlag {
  return {
    id: `review-flag-${index}`,
    label: "Verificare necesară",
    description,
    severity: "warning",
    sectionId: "assessment",
  };
}

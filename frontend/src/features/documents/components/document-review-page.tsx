"use client";

import { useMemo, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button, ButtonLink } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Checkbox, Textarea } from "@/components/ui/input";
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

type DocumentReviewPageProps = {
  consultationId: string;
  reviewDocument?: DocumentReviewDocument | null;
};

const sectionLabels: Record<SoapSectionId, string> = {
  subjective: "Subiectiv",
  objective: "Obiectiv",
  assessment: "Evaluare",
  plan: "Plan",
};

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
  reviewDocument,
}: DocumentReviewPageProps) {
  const [draft, setDraft] = useState<SoapDraft>(
    reviewDocument?.draft ?? emptyDraft,
  );
  const [transcript, setTranscript] = useState(
    reviewDocument?.transcript.transcriptText ?? "",
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
  const [approvalMessage, setApprovalMessage] = useState<string | null>(null);

  const reviewedSectionCount = Object.values(reviewedSections).filter(Boolean)
    .length;
  const resolvedFlagCount = reviewFlags.filter((flag) => resolvedFlags[flag.id])
    .length;
  const hasDraftContent = Object.values(draft).some((value) => value.trim());
  const allSectionsReviewed = reviewedSectionCount === 4;
  const allFlagsResolved =
    reviewFlags.length === 0 || resolvedFlagCount === reviewFlags.length;
  const canApprove =
    hasDraftContent &&
    allSectionsReviewed &&
    allFlagsResolved &&
    saveState === "saved";

  const documentStatus = useMemo(() => {
    if (!hasDraftContent) {
      return { label: "Draft neîncărcat", variant: "neutral" as const };
    }

    if (canApprove) {
      return { label: "Gata de aprobare", variant: "success" as const };
    }

    if (saveState === "dirty") {
      return { label: "Modificări nesalvate", variant: "warning" as const };
    }

    return { label: "În revizuire", variant: "processing" as const };
  }, [canApprove, hasDraftContent, saveState]);

  function fillMockData() {
    setDraft(mockDraft);
    setTranscript(mockTranscript);
    setReviewFlags(mockFlags);
    setReviewedSections(initialReviewedSections);
    setResolvedFlags({});
    setSaveState("dirty");
    setApprovalMessage(null);
  }

  function updateSection(sectionId: SoapSectionId, value: string) {
    setDraft((current) => ({ ...current, [sectionId]: value }));
    setReviewedSections((current) => ({ ...current, [sectionId]: false }));
    setSaveState("dirty");
    setApprovalMessage(null);
  }

  function toggleSectionReview(sectionId: SoapSectionId, checked: boolean) {
    setReviewedSections((current) => ({ ...current, [sectionId]: checked }));
    setApprovalMessage(null);
  }

  function toggleFlag(flagId: string, checked: boolean) {
    setResolvedFlags((current) => ({ ...current, [flagId]: checked }));
    setApprovalMessage(null);
  }

  function saveDraft() {
    if (!hasDraftContent) {
      return;
    }

    setSaveState("saved");
    setApprovalMessage("Draft salvat local pentru demonstrație.");
  }

  function approveDraft() {
    if (!canApprove) {
      return;
    }

    setApprovalMessage("Document pregătit pentru aprobarea reală.");
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 sm:py-8 lg:px-8">
      <header className="mb-5 border-b border-border pb-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0">
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <Badge variant={documentStatus.variant}>
                {documentStatus.label}
              </Badge>
              <Badge variant="neutral">SOAP</Badge>
              <span className="caption-text">
                Consultație {shortId(consultationId)}
              </span>
            </div>
            <h1 className="page-title">Revizuire document clinic</h1>
            <p className="secondary-text mt-2 max-w-3xl">
              Draft generat AI, pregătit pentru corectură medicală și aprobare.
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button type="button" variant="outline" onClick={fillMockData}>
              Completează demo
            </Button>
            <ButtonLink
              href={`/consultations/${consultationId}`}
              variant="outline"
            >
              Înapoi la consultație
            </ButtonLink>
          </div>
        </div>
      </header>

      <StatusStrip
        reviewedSectionCount={reviewedSectionCount}
        resolvedFlagCount={resolvedFlagCount}
        flagCount={reviewFlags.length}
        saveState={saveState}
      />

      <div className="mt-6 grid min-w-0 gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <main className="min-w-0">
          <section className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
            <div className="border-b border-border bg-surface-muted px-4 py-3 sm:px-5">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-sm font-semibold text-foreground">
                    Draft SOAP
                  </h2>
                  <p className="caption-text mt-1">
                    Versiune generată AI, neaprobată
                  </p>
                </div>
                <Badge variant={saveState === "saved" ? "success" : "warning"}>
                  {saveState === "saved" ? "Salvat" : "Nesalvat"}
                </Badge>
              </div>
            </div>

            <div className="divide-y divide-border">
              {(Object.keys(sectionLabels) as SoapSectionId[]).map(
                (sectionId) => (
                  <SoapSectionEditor
                    key={sectionId}
                    sectionId={sectionId}
                    label={sectionLabels[sectionId]}
                    value={draft[sectionId]}
                    reviewed={reviewedSections[sectionId]}
                    onChange={(value) => updateSection(sectionId, value)}
                    onReviewedChange={(checked) =>
                      toggleSectionReview(sectionId, checked)
                    }
                  />
                ),
              )}
            </div>
          </section>
        </main>

        <aside className="min-w-0 space-y-4 xl:sticky xl:top-24 xl:self-start">
          <ApprovalPanel
            reviewedSectionCount={reviewedSectionCount}
            resolvedFlagCount={resolvedFlagCount}
            flagCount={reviewFlags.length}
            saveState={saveState}
            canApprove={canApprove}
            hasDraftContent={hasDraftContent}
            onSave={saveDraft}
            onApprove={approveDraft}
          />

          {approvalMessage ? (
            <Alert
              variant={canApprove ? "success" : "info"}
              title={canApprove ? "Verificare completă" : "Status"}
            >
              {approvalMessage}
            </Alert>
          ) : null}

          <ReviewFlagsPanel
            flags={reviewFlags}
            resolvedFlags={resolvedFlags}
            onResolvedChange={toggleFlag}
          />

          <TranscriptPanel transcript={transcript} />
        </aside>
      </div>
    </div>
  );
}

function StatusStrip({
  reviewedSectionCount,
  resolvedFlagCount,
  flagCount,
  saveState,
}: {
  reviewedSectionCount: number;
  resolvedFlagCount: number;
  flagCount: number;
  saveState: SaveState;
}) {
  return (
    <div className="grid gap-3 sm:grid-cols-3">
      <StatusItem
        label="Secțiuni revizuite"
        value={`${reviewedSectionCount}/4`}
        complete={reviewedSectionCount === 4}
      />
      <StatusItem
        label="Marcaje verificate"
        value={`${resolvedFlagCount}/${flagCount}`}
        complete={flagCount === 0 || resolvedFlagCount === flagCount}
      />
      <StatusItem
        label="Stare salvare"
        value={saveState === "saved" ? "Salvat" : "Nesalvat"}
        complete={saveState === "saved"}
      />
    </div>
  );
}

function StatusItem({
  label,
  value,
  complete,
}: {
  label: string;
  value: string;
  complete: boolean;
}) {
  return (
    <div className="flex min-h-16 items-center justify-between gap-4 rounded-[var(--radius-control)] border border-border bg-surface px-4 py-3 shadow-surface">
      <span className="text-sm font-medium text-foreground">{label}</span>
      <Badge variant={complete ? "success" : "warning"}>{value}</Badge>
    </div>
  );
}

function SoapSectionEditor({
  sectionId,
  label,
  value,
  reviewed,
  onChange,
  onReviewedChange,
}: {
  sectionId: SoapSectionId;
  label: string;
  value: string;
  reviewed: boolean;
  onChange: (value: string) => void;
  onReviewedChange: (checked: boolean) => void;
}) {
  const textareaId = `soap-${sectionId}`;

  return (
    <section className="grid gap-4 px-4 py-5 sm:px-5 lg:grid-cols-[11rem_minmax(0,1fr)]">
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
          className="min-h-32 border-border bg-white text-[0.95rem] leading-7 shadow-none"
        />
        <div className="mt-3 flex flex-col gap-2 border-t border-border pt-3 sm:flex-row sm:items-start sm:justify-between">
          <Checkbox
            label={`Secțiunea ${label} este verificată`}
            checked={reviewed}
            onChange={(event) => onReviewedChange(event.target.checked)}
            disabled={!value.trim()}
          />
          <span className="caption-text sm:text-right">
            Editarea resetează verificarea.
          </span>
        </div>
      </div>
    </section>
  );
}

function ApprovalPanel({
  reviewedSectionCount,
  resolvedFlagCount,
  flagCount,
  saveState,
  canApprove,
  hasDraftContent,
  onSave,
  onApprove,
}: {
  reviewedSectionCount: number;
  resolvedFlagCount: number;
  flagCount: number;
  saveState: SaveState;
  canApprove: boolean;
  hasDraftContent: boolean;
  onSave: () => void;
  onApprove: () => void;
}) {
  const checks = [
    {
      label: "SOAP",
      detail: `${reviewedSectionCount}/4 secțiuni`,
      complete: reviewedSectionCount === 4,
    },
    {
      label: "Marcaje",
      detail: `${resolvedFlagCount}/${flagCount} verificate`,
      complete: flagCount === 0 || resolvedFlagCount === flagCount,
    },
    {
      label: "Salvare",
      detail: saveState === "saved" ? "draft salvat" : "draft nesalvat",
      complete: saveState === "saved",
    },
  ];

  return (
    <Card>
      <CardHeader className="border-b border-border pb-4">
        <h2 className="section-title">Aprobare</h2>
      </CardHeader>
      <CardContent className="pt-4">
        <ul className="space-y-1">
          {checks.map((check) => (
            <li
              key={check.label}
              className="flex items-center justify-between gap-3 py-2"
            >
              <span className="min-w-0">
                <span className="block text-sm font-medium text-foreground">
                  {check.label}
                </span>
                <span className="caption-text block">{check.detail}</span>
              </span>
              <span
                className={cn(
                  "size-2.5 rounded-full",
                  check.complete ? "bg-success" : "bg-warning",
                )}
                aria-hidden="true"
              />
            </li>
          ))}
        </ul>

        <div className="mt-5 grid gap-2">
          <Button
            type="button"
            variant="primary"
            onClick={onSave}
            disabled={!hasDraftContent || saveState === "saved"}
          >
            Salvează draftul
          </Button>
          <Button
            type="button"
            variant="success"
            onClick={onApprove}
            disabled={!canApprove}
          >
            Aprobă documentul
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function ReviewFlagsPanel({
  flags,
  resolvedFlags,
  onResolvedChange,
}: {
  flags: ReviewFlag[];
  resolvedFlags: Record<string, boolean>;
  onResolvedChange: (flagId: string, checked: boolean) => void;
}) {
  return (
    <Card>
      <CardHeader className="border-b border-border pb-4">
        <div className="flex items-center justify-between gap-3">
          <h2 className="section-title">Marcaje clinice</h2>
          <Badge variant={flags.length ? "warning" : "neutral"}>
            {flags.length}
          </Badge>
        </div>
      </CardHeader>
      <CardContent className="pt-4">
        {flags.length === 0 ? (
          <p className="rounded-[var(--radius-control)] border border-border bg-surface-muted p-3 text-sm text-muted-foreground">
            Niciun marcaj încă.
          </p>
        ) : (
          <div className="space-y-3">
            {flags.map((flag) => (
              <div
                key={flag.id}
                className={cn(
                  "rounded-[var(--radius-control)] border bg-surface p-3",
                  flag.severity === "destructive"
                    ? "border-destructive/30"
                    : "border-warning/35",
                )}
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
                  onChange={(event) =>
                    onResolvedChange(flag.id, event.target.checked)
                  }
                />
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function TranscriptPanel({ transcript }: { transcript: string }) {
  return (
    <Card variant="muted">
      <CardHeader className="border-b border-border pb-4">
        <h2 className="section-title">Transcriere</h2>
      </CardHeader>
      <CardContent className="pt-4">
        {transcript ? (
          <div className="max-h-80 overflow-y-auto rounded-[var(--radius-control)] border border-border bg-surface p-4">
            <pre className="whitespace-pre-wrap font-sans text-sm leading-6 text-foreground">
              {transcript}
            </pre>
          </div>
        ) : (
          <p className="rounded-[var(--radius-control)] border border-border bg-surface p-3 text-sm text-muted-foreground">
            Nicio transcriere încă.
          </p>
        )}
      </CardContent>
    </Card>
  );
}

function shortId(value: string) {
  return value.length > 8 ? value.slice(0, 8) : value;
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

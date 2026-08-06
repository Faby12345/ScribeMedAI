"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import {
  GetConsultationsApiError,
  getConsultations,
} from "@/features/consultations/api/get-consultations";
import { NewConsultationPanel } from "@/features/consultations/components/new-consultation-panel";
import type {
  Consultation,
  ConsultationStatus,
  PaginatedResponse,
} from "@/features/consultations/types";
import { cn } from "@/lib/class-names";

const activityLimit = 50;

const consultationPriority: Record<ConsultationStatus, number> = {
  TRANSCRIPTION_FAILED: 0,
  TRANSCRIPTION_READY: 1,
  TRANSCRIBING: 2,
  AUDIO_UPLOADED: 3,
  PATIENT_INFORMED: 4,
  CREATED: 5,
};

const statusLabels: Record<ConsultationStatus, string> = {
  CREATED: "Începută",
  PATIENT_INFORMED: "Pregătită pentru audio",
  AUDIO_UPLOADED: "Audio încărcat",
  TRANSCRIBING: "În transcriere",
  TRANSCRIPTION_READY: "Gata pentru revizuire",
  TRANSCRIPTION_FAILED: "Transcriere eșuată",
};

const statusVariants: Record<
  ConsultationStatus,
  "neutral" | "info" | "processing" | "success" | "warning" | "destructive"
> = {
  CREATED: "neutral",
  PATIENT_INFORMED: "info",
  AUDIO_UPLOADED: "info",
  TRANSCRIBING: "processing",
  TRANSCRIPTION_READY: "warning",
  TRANSCRIPTION_FAILED: "destructive",
};

const nextActionLabels: Record<ConsultationStatus, string> = {
  CREATED: "Continuă",
  PATIENT_INFORMED: "Continuă",
  AUDIO_UPLOADED: "Continuă",
  TRANSCRIBING: "Vezi status",
  TRANSCRIPTION_READY: "Revizuiește",
  TRANSCRIPTION_FAILED: "Deschide",
};

const statusToneClasses: Record<ConsultationStatus, string> = {
  CREATED: "bg-muted-foreground",
  PATIENT_INFORMED: "bg-info",
  AUDIO_UPLOADED: "bg-info",
  TRANSCRIBING: "bg-info motion-safe:animate-pulse",
  TRANSCRIPTION_READY: "bg-warning",
  TRANSCRIPTION_FAILED: "bg-destructive",
};

export function DashboardShell() {
  const [isNewConsultationOpen, setIsNewConsultationOpen] = useState(false);
  const startConsultationButtonRef = useRef<HTMLButtonElement>(null);

  return (
    <>
      <div className="mx-auto grid w-full max-w-7xl gap-8 px-4 py-6 sm:px-6 sm:py-8 lg:grid-cols-[minmax(18rem,0.68fr)_minmax(0,1.32fr)] lg:px-8">
        <section
          aria-labelledby="ai-workspace-title"
          className="relative overflow-hidden rounded-xl bg-white/78"
        >
          <div
            className="pointer-events-none absolute inset-0 bg-[linear-gradient(135deg,#ffffff_0%,#eef9ff_58%,#ffffff_100%)]"
            aria-hidden="true"
          />
          <div
            className="pointer-events-none absolute inset-x-6 top-0 h-px bg-[linear-gradient(90deg,transparent,#9fdcf1,transparent)]"
            aria-hidden="true"
          />
          <div className="relative px-1 py-2 sm:px-2 sm:py-4">
            <h1
              id="ai-workspace-title"
              className="max-w-xl text-2xl font-semibold leading-tight tracking-normal text-foreground sm:text-3xl"
            >
              Ce documentăm acum?
            </h1>
            <p className="mt-3 max-w-xl text-sm leading-6 text-muted-foreground">
              Selectează pacientul, confirmă identitatea și deschide fluxul de
              consultație asistat de AI.
            </p>

            <div className="mt-6">
              <Button
                ref={startConsultationButtonRef}
                type="button"
                variant="primary"
                className="w-full sm:w-auto"
                onClick={() => setIsNewConsultationOpen(true)}
              >
                Începe consultația
              </Button>
            </div>

            <div className="mt-5 flex flex-wrap gap-x-4 gap-y-2 text-sm">
              <Link
                href="/patients/new"
                className="font-medium text-primary underline-offset-4 transition-colors hover:text-primary-hover hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
              >
                Pacient nou
              </Link>
              <Link
                href="/patients"
                className="font-medium text-muted-foreground underline-offset-4 transition-colors hover:text-foreground hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
              >
                Registru pacienți
              </Link>
            </div>
          </div>
        </section>

        <section aria-labelledby="activity-title" className="min-w-0">
          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 id="activity-title" className="section-title">
                Activitate care cere atenție
              </h2>
              <p className="secondary-text mt-1">
                Consultațiile recente sunt afișate după acțiunea necesară.
              </p>
            </div>
            <Link
              href="/consultations"
              className="text-sm font-medium text-muted-foreground underline-offset-4 transition-colors hover:text-foreground hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
            >
              Toate consultațiile
            </Link>
          </div>
          <ConsultationActivity />
        </section>
      </div>

      <NewConsultationPanel
        isOpen={isNewConsultationOpen}
        onClose={() => setIsNewConsultationOpen(false)}
        returnFocusRef={startConsultationButtonRef}
      />
    </>
  );
}

function ConsultationActivity() {
  const [consultationsPage, setConsultationsPage] =
    useState<PaginatedResponse<Consultation> | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    async function loadConsultations() {
      setIsLoading(true);
      setError(null);

      try {
        const nextPage = await getConsultations({
          page: 0,
          size: activityLimit,
        });

        if (isActive) {
          setConsultationsPage(nextPage);
        }
      } catch (loadError) {
        if (!isActive) {
          return;
        }

        if (loadError instanceof GetConsultationsApiError) {
          setError(loadError.message);
        } else {
          setError("Activitatea consultațiilor nu a putut fi încărcată.");
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    loadConsultations();

    return () => {
      isActive = false;
    };
  }, [retryKey]);

  const prioritizedConsultations = useMemo(() => {
    if (!consultationsPage) {
      return [];
    }

    return [...consultationsPage.content].sort((first, second) => {
      const priorityDifference =
        consultationPriority[first.status] - consultationPriority[second.status];

      if (priorityDifference !== 0) {
        return priorityDifference;
      }

      return (
        new Date(second.createdAt).getTime() -
        new Date(first.createdAt).getTime()
      );
    });
  }, [consultationsPage]);

  if (isLoading) {
    return <ActivitySkeleton />;
  }

  if (error) {
    return (
      <Alert variant="error" title="Nu am putut încărca activitatea">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <span>{error}</span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => setRetryKey((current) => current + 1)}
          >
            Reîncearcă
          </Button>
        </div>
      </Alert>
    );
  }

  if (!consultationsPage || consultationsPage.totalElements === 0) {
    return (
      <EmptyState
        title="Nu există consultații încă."
        description="Începe cu pacientul potrivit. Consultațiile create vor apărea aici pentru continuare și revizuire."
      />
    );
  }

  return (
    <div className="overflow-hidden rounded-xl bg-white/82">
      <ul className="divide-y divide-border/75 border-y border-border/75">
        {prioritizedConsultations.map((consultation) => (
          <ActivityItem key={consultation.id} consultation={consultation} />
        ))}
      </ul>
    </div>
  );
}

function ActivityItem({ consultation }: { consultation: Consultation }) {
  return (
    <li>
      <Link
        href={`/consultations/${consultation.id}`}
        className="grid gap-3 px-1 py-4 transition-colors hover:bg-white/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center sm:px-3"
      >
        <div className="min-w-0">
          <div className="flex min-w-0 flex-wrap items-center gap-2">
            <StatusDot status={consultation.status} />
            <p className="truncate text-sm font-semibold text-foreground">
              {formatPatientName(consultation)}
            </p>
            <Badge variant={statusVariants[consultation.status]}>
              {statusLabels[consultation.status]}
            </Badge>
          </div>
          <dl className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
            <ActivityMeta label="Dată" value={formatDateTime(consultation.createdAt)} />
            <ActivityMeta
              label="Actualizare"
              value={formatDateTime(consultation.updatedAt)}
            />
          </dl>
        </div>

        <span className="inline-flex h-9 items-center justify-center rounded-[var(--radius-control)] border border-border bg-white px-3 text-sm font-medium text-foreground">
          {nextActionLabels[consultation.status]}
        </span>
      </Link>
    </li>
  );
}

function ActivityMeta({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-1">
      <dt>{label}:</dt>
      <dd className="text-foreground">{value}</dd>
    </div>
  );
}

function StatusDot({ status }: { status: ConsultationStatus }) {
  return (
    <span
      className={cn("size-2 rounded-full", statusToneClasses[status])}
      aria-hidden="true"
    />
  );
}

function ActivitySkeleton() {
  return (
    <div
      className="overflow-hidden rounded-xl bg-white/82"
      aria-busy="true"
    >
      <div className="divide-y divide-border/75 border-y border-border/75">
        {Array.from({ length: 5 }, (_, index) => (
          <div key={index} className="grid gap-3 px-4 py-4 sm:grid-cols-[1fr_8rem]">
            <Skeleton className="h-10" />
            <Skeleton className="h-9" />
          </div>
        ))}
      </div>
      <p className="secondary-text border-t border-border px-4 py-3">
        Se încarcă activitatea consultațiilor...
      </p>
    </div>
  );
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatPatientName(consultation: Consultation) {
  return `${consultation.patientLastName} ${consultation.patientFirstName}`.trim();
}

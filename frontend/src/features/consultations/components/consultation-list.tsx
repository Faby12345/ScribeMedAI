"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ButtonLink } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import {
  GetConsultationsApiError,
  getConsultations,
} from "@/features/consultations/api/get-consultations";
import type {
  Consultation,
  ConsultationStatus,
  PaginatedResponse,
} from "@/features/consultations/types";

const pageSize = 20;

const statusLabels: Record<ConsultationStatus, string> = {
  CREATED: "Creată",
  PATIENT_INFORMED: "Pacient informat",
  AUDIO_UPLOADED: "Audio încărcat",
  TRANSCRIBING: "În transcriere",
  TRANSCRIPTION_READY: "Transcriere disponibilă",
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

export function ConsultationList() {
  const [consultationsPage, setConsultationsPage] =
    useState<PaginatedResponse<Consultation> | null>(null);
  const [page, setPage] = useState(0);
  const [retryKey, setRetryKey] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    async function loadConsultations() {
      setIsLoading(true);
      setError(null);

      try {
        const nextPage = await getConsultations({ page, size: pageSize });

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
          setError("Lista consultațiilor nu a putut fi încărcată.");
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
  }, [page, retryKey]);

  if (isLoading) {
    return (
      <div className="space-y-3 rounded-xl bg-white/82 p-4" aria-busy="true">
        <Skeleton className="h-4 w-44" />
        <Skeleton className="h-14" />
        <Skeleton className="h-14" />
        <Skeleton className="h-14" />
        <p className="secondary-text">Se încarcă consultațiile...</p>
      </div>
    );
  }

  if (error) {
    return (
      <Alert variant="error" title="Nu am putut încărca consultațiile">
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
        description="Creează prima consultație din panoul clinic după alegerea pacientului."
        action={
          <ButtonLink href="/dashboard">
            Mergi la panou
          </ButtonLink>
        }
      />
    );
  }

  return (
    <div className="space-y-4">
      <div className="overflow-hidden rounded-xl bg-white/82">
        <ul className="divide-y divide-border/75 border-y border-border/75">
          {consultationsPage.content.map((consultation) => (
            <ConsultationListItem
              key={consultation.id}
              consultation={consultation}
            />
          ))}
        </ul>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="secondary-text">
          Pagina {consultationsPage.number + 1} din{" "}
          {Math.max(consultationsPage.totalPages, 1)} ·{" "}
          {consultationsPage.totalElements} consultații
        </p>
        <div className="flex gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={consultationsPage.first}
            onClick={() => setPage((current) => Math.max(current - 1, 0))}
          >
            Anterioară
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={consultationsPage.last}
            onClick={() => setPage((current) => current + 1)}
          >
            Următoare
          </Button>
        </div>
      </div>
    </div>
  );
}

function ConsultationListItem({
  consultation,
}: {
  consultation: Consultation;
}) {
  return (
    <li>
      <Link
        href={`/consultations/${consultation.id}`}
        className="grid gap-3 px-1 py-4 transition-colors hover:bg-white/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center sm:px-3"
      >
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <p className="truncate text-sm font-semibold text-foreground">
              {formatPatientName(consultation)}
            </p>
            <Badge variant={statusVariants[consultation.status]}>
              {statusLabels[consultation.status]}
            </Badge>
          </div>
          <dl className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
            <ConsultationMeta
              label="Creată"
              value={formatDateTime(consultation.createdAt)}
            />
            <ConsultationMeta
              label="Actualizată"
              value={formatDateTime(consultation.updatedAt)}
            />
          </dl>
        </div>
        <span className="inline-flex h-9 items-center justify-center rounded-[var(--radius-control)] border border-border bg-white px-3 text-sm font-medium text-foreground">
          Deschide
        </span>
      </Link>
    </li>
  );
}

function ConsultationMeta({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-1">
      <dt>{label}:</dt>
      <dd className="text-foreground">{value}</dd>
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

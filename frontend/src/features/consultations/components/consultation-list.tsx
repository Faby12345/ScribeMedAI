"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button, ButtonLink } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { SearchInput } from "@/components/ui/search-input";
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
  NOTES_PROCESSING: "Draft în generare",
  NOTES_READY: "Draft disponibil",
  NOTES_FAILED: "Generare draft eșuată",
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
  NOTES_PROCESSING: "processing",
  NOTES_READY: "success",
  NOTES_FAILED: "destructive",
};

export function ConsultationList() {
  const [consultationsPage, setConsultationsPage] =
    useState<PaginatedResponse<Consultation> | null>(null);
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
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

  const visibleConsultations = useMemo(() => {
    const consultations = consultationsPage?.content ?? [];
    const normalizedQuery = query.trim().toLocaleLowerCase("ro-RO");

    if (!normalizedQuery) {
      return consultations;
    }

    return consultations.filter((consultation) => {
      const searchableText = [
        consultation.patientFirstName,
        consultation.patientLastName,
        consultation.status,
        statusLabels[consultation.status],
      ]
        .filter(Boolean)
        .join(" ")
        .toLocaleLowerCase("ro-RO");

      return searchableText.includes(normalizedQuery);
    });
  }, [consultationsPage, query]);

  if (isLoading) {
    return <ConsultationListSkeleton />;
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
      <div className="flex flex-col gap-3 rounded-[var(--radius-surface)] border border-border bg-surface p-4 shadow-surface sm:flex-row sm:items-end sm:justify-between">
        <label className="min-w-0 flex-1 space-y-2">
          <SearchInput
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Pacient sau status"
            aria-label="Caută consultație în pagina încărcată"
          />
        </label>
        <ButtonLink href="/dashboard" variant="primary">
          Consultație nouă
        </ButtonLink>
      </div>

      {consultationsPage.content.length > 0 &&
      visibleConsultations.length === 0 ? (
        <EmptyState
          title="Nu am găsit consultații pentru această căutare."
          description="Căutarea se aplică pe pagina de consultații încărcată."
        />
      ) : null}

      {visibleConsultations.length > 0 ? (
        <div className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
          <div className="hidden grid-cols-[minmax(14rem,1.2fr)_minmax(11rem,0.8fr)_minmax(11rem,0.8fr)_minmax(11rem,0.8fr)_7rem] gap-4 border-b border-border bg-surface-muted px-4 py-3 text-xs font-semibold uppercase text-muted-foreground md:grid">
            <span>Pacient</span>
            <span>Status</span>
            <span>Creată</span>
            <span>Actualizată</span>
            <span className="text-right">Acțiune</span>
          </div>
          <ul className="divide-y divide-border">
          {visibleConsultations.map((consultation) => (
            <ConsultationListItem
              key={consultation.id}
              consultation={consultation}
            />
          ))}
        </ul>
      </div>
      ) : null}

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
        className="grid gap-3 px-4 py-4 transition-colors hover:bg-surface-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring md:grid-cols-[minmax(14rem,1.2fr)_minmax(11rem,0.8fr)_minmax(11rem,0.8fr)_minmax(11rem,0.8fr)_7rem] md:items-center"
      >
        <div className="flex min-w-0 items-center gap-3">
          <ConsultationDocumentIcon />
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="truncate text-sm font-semibold text-foreground">
                {formatPatientName(consultation)}
              </p>
            </div>
          </div>
        </div>

        {/*<div className="min-w-0">*/}
          {/*<div className="flex flex-wrap items-center gap-2">
            <p className="truncate text-sm font-semibold text-foreground">
              {formatPatientName(consultation)}
            </p>
          </div>*/}
          {/*<p className="caption-text mt-1">Consultație clinică</p>*/}
        {/*</div>*/}

        <div>
          <Badge variant={statusVariants[consultation.status]}>
            {statusLabels[consultation.status]}
          </Badge>
        </div>

        <ConsultationMeta
          label="Creată"
          value={formatDateTime(consultation.createdAt)}
        />

        <ConsultationMeta
          label="Actualizată"
          value={formatDateTime(consultation.updatedAt)}
        />

        <span className="inline-flex h-9 items-center justify-center rounded-[var(--radius-control)] border border-border bg-surface px-3 text-sm font-medium text-foreground shadow-surface md:justify-self-end">
          Deschide
        </span>
      </Link>
    </li>
  );
}

function ConsultationDocumentIcon() {
  return (
    <span
      className="flex size-10 shrink-0 items-center justify-center text-primary"
      aria-hidden="true"
    >
      <svg
        className="size-5"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      >
        <path d="M7.75 3.75h6.7l3.8 3.8v12.7H7.75a2 2 0 0 1-2-2V5.75a2 2 0 0 1 2-2Z" />
        <path d="M14.25 3.9v3.6a1 1 0 0 0 1 1h3.1" />
        <path d="M9.25 12.25h5.5" />
        <path d="M9.25 15.75h4.25" />
      </svg>
    </span>
  );
}

function ConsultationMeta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="caption-text md:hidden">{label}</dt>
      <dd className="mt-1 text-sm text-foreground md:mt-0">{value}</dd>
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

function ConsultationListSkeleton() {
  return (
    <div className="space-y-4" aria-busy="true">
      <div className="rounded-[var(--radius-surface)] border border-border bg-surface p-4 shadow-surface">
        <Skeleton className="h-4 w-36" />
        <Skeleton className="mt-3 h-[var(--control-height)]" />
      </div>
      <div className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
        <div className="divide-y divide-border">
          {Array.from({ length: 5 }, (_, index) => (
            <div
              key={index}
              className="grid gap-3 px-4 py-4 md:grid-cols-[minmax(14rem,1.2fr)_minmax(11rem,0.8fr)_minmax(11rem,0.8fr)_minmax(11rem,0.8fr)_7rem]"
            >
              <Skeleton className="h-10" />
              <Skeleton className="h-9" />
              <Skeleton className="h-10" />
              <Skeleton className="h-10" />
              <Skeleton className="h-9" />
            </div>
          ))}
        </div>
        <p className="secondary-text px-4 py-3">Se încarcă consultațiile...</p>
      </div>
    </div>
  );
}

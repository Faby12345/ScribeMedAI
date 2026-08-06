"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { SearchInput } from "@/components/ui/search-input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  GetPatientsApiError,
  getPatients,
} from "@/features/patients/api/get-patients";
import { PatientAvatar } from "@/features/patients/components/patient-avatar";
import {
  ageFromBirthDate,
  formatDate,
  patientDisplayName,
  sexLabels,
  statusLabels,
} from "@/features/patients/components/patient-formatters";
import type { Patient } from "@/features/patients/types";

type PatientListProps = {
  refreshKey?: number;
  onCreatePatient?: () => void;
  createPatientHref?: string;
};

export function PatientList({
  refreshKey,
  onCreatePatient,
  createPatientHref = "/patients/new",
}: PatientListProps) {
  const [patients, setPatients] = useState<Patient[]>([]);
  const [query, setQuery] = useState("");
  const [retryKey, setRetryKey] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    async function loadPatients() {
      setIsLoading(true);
      setError(null);

      try {
        const nextPatients = await getPatients();

        if (isActive) {
          setPatients(nextPatients);
        }
      } catch (loadError) {
        if (!isActive) {
          return;
        }

        if (loadError instanceof GetPatientsApiError) {
          setError(loadError.message);
        } else {
          setError("Lista pacienților nu a putut fi încărcată.");
        }
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    loadPatients();

    return () => {
      isActive = false;
    };
  }, [refreshKey, retryKey]);

  const visiblePatients = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("ro-RO");

    if (!normalizedQuery) {
      return patients;
    }

    return patients.filter((patient) => {
      const searchableText = [
        patient.firstName,
        patient.lastName,
        patient.phone,
        patient.email,
      ]
        .filter(Boolean)
        .join(" ")
        .toLocaleLowerCase("ro-RO");

      return searchableText.includes(normalizedQuery);
    });
  }, [patients, query]);

  if (isLoading) {
    return <PatientListSkeleton />;
  }

  if (error) {
    return (
      <Alert variant="error" title="Nu am putut încărca pacienții">
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

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 rounded-[var(--radius-surface)] border border-border bg-surface p-4 shadow-surface sm:flex-row sm:items-end sm:justify-between">
        <label className="min-w-0 flex-1 space-y-2">
          <span className="text-sm font-medium text-foreground">
            Caută pacient
          </span>
          <SearchInput
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Nume, telefon sau email"
            aria-label="Caută pacient în lista încărcată"
          />
        </label>
        {onCreatePatient ? (
          <Button type="button" variant="primary" onClick={onCreatePatient}>
            Adaugă pacient
          </Button>
        ) : (
          <Link
            href={createPatientHref}
            className="inline-flex h-[var(--control-height)] items-center justify-center rounded-[var(--radius-control)] border border-primary bg-primary px-4 text-sm font-medium text-primary-foreground shadow-surface transition-colors hover:bg-primary-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            Adaugă pacient
          </Link>
        )}
      </div>

      {patients.length === 0 ? (
        <EmptyState
          title="Nu există pacienți încă."
          description="Adaugă primul pacient pentru a începe fluxul de consultație."
          action={
            onCreatePatient ? (
              <Button
                type="button"
                variant="primary"
                onClick={onCreatePatient}
              >
                Adaugă pacient
              </Button>
            ) : (
              <Link
                href={createPatientHref}
                className="inline-flex h-[var(--control-height)] items-center justify-center rounded-[var(--radius-control)] border border-primary bg-primary px-4 text-sm font-medium text-primary-foreground shadow-surface transition-colors hover:bg-primary-hover focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
              >
                Adaugă pacient
              </Link>
            )
          }
        />
      ) : null}

      {patients.length > 0 && visiblePatients.length === 0 ? (
        <EmptyState
          title="Nu am găsit pacienți pentru această căutare."
          description="Verifică numele, telefonul sau adresa de email."
        />
      ) : null}

      {visiblePatients.length > 0 ? (
        <div className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
          <div className="hidden grid-cols-[minmax(15rem,1.4fr)_minmax(10rem,0.8fr)_minmax(12rem,1fr)_minmax(10rem,0.8fr)_7rem] gap-4 border-b border-border bg-surface-muted px-4 py-3 text-xs font-semibold uppercase text-muted-foreground md:grid">
            <span>Pacient</span>
            <span>Naștere</span>
            <span>Contact</span>
            <span>Ultima consultație</span>
            <span className="text-right">Acțiune</span>
          </div>
          <ul className="divide-y divide-border">
            {visiblePatients.map((patient) => (
              <PatientListItem key={patient.id} patient={patient} />
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  );
}

export default function PatientListItem({ patient }: { patient: Patient }) {
  const age = ageFromBirthDate(patient.birthDate);

  return (
    <li>
      <Link
        href={`/patients/${patient.id}`}
        className="grid gap-3 px-4 py-4 transition-colors hover:bg-surface-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring md:grid-cols-[minmax(15rem,1.4fr)_minmax(10rem,0.8fr)_minmax(12rem,1fr)_minmax(10rem,0.8fr)_7rem] md:items-center"
      >
        <div className="flex min-w-0 items-center gap-3">
          <PatientAvatar patient={patient} />
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <p className="truncate text-sm font-semibold text-foreground">
                {patientDisplayName(patient)}
              </p>
              <Badge variant={patient.status === "ACTIVE" ? "info" : "neutral"}>
                {statusLabels[patient.status]}
              </Badge>
            </div>
          </div>
        </div>

        <div>
          <p className="text-sm text-foreground">{formatDate(patient.birthDate)}</p>
          <p className="caption-text mt-1">
            {age === null ? "Vârsta nespecificată" : `${age} ani`}
          </p>
        </div>

        <dl className="space-y-1 text-sm text-muted-foreground">
          <PatientMeta label="Telefon" value={patient.phone} />
          <PatientMeta label="Email" value={patient.email} />
          <PatientMeta
            label="Sex"
            value={patient.sex ? sexLabels[patient.sex] : null}
          />
        </dl>

        <p className="text-sm text-muted-foreground">Indisponibilă</p>

        <span className="inline-flex h-9 items-center justify-center rounded-[var(--radius-control)] border border-border bg-surface px-3 text-sm font-medium text-foreground shadow-surface md:justify-self-end">
          Deschide
        </span>
      </Link>
    </li>
  );
}

function PatientMeta({
  label,
  value,
}: {
  label: string;
  value: string | null;
}) {
  if (!value) {
    return null;
  }

  return (
    <div className="flex gap-1">
      <dt>{label}:</dt>
      <dd className="text-foreground">{value}</dd>
    </div>
  );
}

function PatientListSkeleton() {
  return (
    <div className="space-y-4" aria-busy="true">
      <div className="rounded-[var(--radius-surface)] border border-border bg-surface p-4 shadow-surface">
        <Skeleton className="h-4 w-32" />
        <Skeleton className="mt-3 h-[var(--control-height)]" />
      </div>
      <div className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
        <div className="divide-y divide-border">
          {Array.from({ length: 5 }, (_, index) => (
            <div
              key={index}
              className="grid gap-3 px-4 py-4 md:grid-cols-[minmax(15rem,1.4fr)_minmax(10rem,0.8fr)_minmax(12rem,1fr)_minmax(10rem,0.8fr)_7rem]"
            >
              <Skeleton className="h-10" />
              <Skeleton className="h-10" />
              <Skeleton className="h-10" />
              <Skeleton className="h-10" />
              <Skeleton className="h-9" />
            </div>
          ))}
        </div>
        <p className="secondary-text px-4 py-3">Se încarcă pacienții...</p>
      </div>
    </div>
  );
}

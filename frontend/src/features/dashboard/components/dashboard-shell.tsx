"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button, ButtonLink } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { NewConsultationPanel } from "@/features/consultations/components/new-consultation-panel";
import {
  GetPatientsApiError,
  getPatients,
} from "@/features/patients/api/get-patients";
import { PatientAvatar } from "@/features/patients/components/patient-avatar";
import {
  formatDateTime as formatPatientDateTime,
  patientDisplayName,
  statusLabels as patientStatusLabels,
} from "@/features/patients/components/patient-formatters";
import type { Patient } from "@/features/patients/types";

export function DashboardShell() {
  const [isNewConsultationOpen, setIsNewConsultationOpen] = useState(false);
  const startConsultationButtonRef = useRef<HTMLButtonElement>(null);

  return (
    <>
      <div className="mx-auto w-full max-w-7xl space-y-9 px-4 py-6 sm:px-6 sm:py-8 lg:px-8">
        <section
          aria-labelledby="ai-workspace-title"
          className="mx-auto max-w-4xl border-b border-border pb-8 text-center"
        >
          <div className="mx-auto max-w-2xl">
            <h1
              id="ai-workspace-title"
              className="page-title"
            >
              Panou clinic
            </h1>
            <p className="secondary-text mx-auto mt-2 max-w-xl">
              Pornește rapid o consultație sau adaugă pacientul înainte de
              documentare.
            </p>
          </div>

          <div className="mx-auto mt-7 grid max-w-3xl gap-3 sm:grid-cols-2">
            <Button
              ref={startConsultationButtonRef}
              type="button"
              variant="primary"
              className="h-auto min-h-28 justify-start p-5 text-left"
              onClick={() => setIsNewConsultationOpen(true)}
            >
              <ConsultationActionIcon />
              <span className="min-w-0">
                <span className="block text-base font-semibold">
                  Consultație nouă
                </span>
                <span className="mt-1 block text-sm font-normal opacity-85">
                  Alege pacientul și deschide fluxul audio.
                </span>
              </span>
            </Button>

            <ButtonLink
              href="/patients/new"
              variant="outline"
              className="h-auto min-h-28 justify-start p-5 text-left"
            >
              <PatientActionIcon />
              <span className="min-w-0">
                <span className="block text-base font-semibold">
                  Adaugă pacient
                </span>
                <span className="mt-1 block text-sm font-normal text-muted-foreground">
                  Creează profilul clinic înainte de consultație.
                </span>
              </span>
            </ButtonLink>
          </div>
        </section>

        <section
          aria-labelledby="recent-patients-title"
          className="mx-auto w-full max-w-4xl"
        >
          <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <h2 id="recent-patients-title" className="section-title">
                Pacienți recenți
              </h2>
              <p className="secondary-text mt-1">
                Ultimii 10 pacienți actualizați în registrul clinic.
              </p>
            </div>
            <Link
              href="/patients"
              className="text-sm font-medium text-muted-foreground underline-offset-4 transition-colors hover:text-foreground hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
            >
              Toți pacienții
            </Link>
          </div>
          <RecentPatientsList />
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

function RecentPatientsList() {
  const [patients, setPatients] = useState<Patient[]>([]);
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
          setError("Lista pacienților recenți nu a putut fi încărcată.");
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
  }, [retryKey]);

  const recentPatients = useMemo(() => {
    return [...patients]
      .sort(
        (first, second) =>
          new Date(second.updatedAt).getTime() -
          new Date(first.updatedAt).getTime(),
      )
      .slice(0, 10);
  }, [patients]);

  if (isLoading) {
    return <RecentPatientsSkeleton />;
  }

  if (error) {
    return (
      <Alert variant="error" title="Nu am putut încărca pacienții recenți">
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

  if (recentPatients.length === 0) {
    return (
      <EmptyState
        title="Nu există pacienți încă."
        description="Adaugă primul pacient pentru a putea crea consultații și documente clinice."
        action={
          <ButtonLink href="/patients/new">
            Adaugă pacient
          </ButtonLink>
        }
      />
    );
  }

  return (
    <div className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
      <ul className="divide-y divide-border">
        {recentPatients.map((patient) => (
          <RecentPatientItem key={patient.id} patient={patient} />
        ))}
      </ul>
    </div>
  );
}

function RecentPatientItem({ patient }: { patient: Patient }) {
  return (
    <li>
      <Link
        href={`/patients/${patient.id}`}
        className="grid gap-3 px-4 py-4 transition-colors hover:bg-surface-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center"
      >
        <div className="flex min-w-0 items-center gap-3">
          <PatientAvatar patient={patient} className="size-9" />
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-foreground">
              {patientDisplayName(patient)}
            </p>
            <p className="caption-text mt-1 truncate">
              {patient.phone || patient.email || "Contact nespecificat"}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 sm:justify-end">
          <span className="caption-text hidden sm:inline">
            {formatPatientDateTime(patient.updatedAt)}
          </span>
        </div>
      </Link>
    </li>
  );
}

function ConsultationActionIcon() {
  return (
    <span
      className="flex size-10 shrink-0 items-center justify-center"
      aria-hidden="true"
    >
      <svg
        className="size-6"
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

function PatientActionIcon() {
  return (
    <span
      className="flex size-10 shrink-0 items-center justify-center text-primary"
      aria-hidden="true"
    >
      <svg
        className="size-6"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      >
        <path d="M12 12.25a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
        <path d="M4.75 20.25a7.25 7.25 0 0 1 14.5 0" />
        <path d="M19.25 5.75v4.5" />
        <path d="M21.5 8h-4.5" />
      </svg>
    </span>
  );
}

function RecentPatientsSkeleton() {
  return (
    <div
      className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface"
      aria-busy="true"
    >
      <div className="divide-y divide-border">
        {Array.from({ length: 5 }, (_, index) => (
          <div
            key={index}
            className="grid gap-3 px-4 py-4 sm:grid-cols-[minmax(0,1fr)_8rem]"
          >
            <div className="flex items-center gap-3">
              <Skeleton className="size-9" />
              <div className="min-w-0 flex-1">
                <Skeleton className="h-4 w-40" />
                <Skeleton className="mt-2 h-3 w-28" />
              </div>
            </div>
            <Skeleton className="h-8" />
          </div>
        ))}
      </div>
      <p className="secondary-text border-t border-border px-4 py-3">
        Se încarcă pacienții recenți...
      </p>
    </div>
  );
}

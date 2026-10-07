"use client";

import Link from "next/link";
import { useEffect, useMemo, useRef, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Button, ButtonLink } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { NewConsultationPanel } from "@/features/consultations/components/new-consultation-panel";
import {
  GetConsultationsApiError,
  getConsultations,
} from "@/features/consultations/api/get-consultations";
import type {
  Consultation,
  PaginatedResponse,
} from "@/features/consultations/types";
import {
  GetPatientsApiError,
  getPatients,
} from "@/features/patients/api/get-patients";
import { PatientAvatar } from "@/features/patients/components/patient-avatar";
import {
  formatDateTime as formatPatientDateTime,
  patientDisplayName,
} from "@/features/patients/components/patient-formatters";
import type { Patient } from "@/features/patients/types";

export function DashboardShell() {
  const [isNewConsultationOpen, setIsNewConsultationOpen] = useState(false);
  const startConsultationButtonRef = useRef<HTMLButtonElement>(null);

  return (
    <>
      <div className="dashboard-background min-h-[calc(100vh-4.25rem)]">
        <div className="mx-auto w-full max-w-7xl space-y-10 px-4 py-7 sm:px-6 sm:py-10 lg:px-8 lg:py-12">
          <section
            aria-labelledby="ai-workspace-title"
            className="mx-auto max-w-4xl text-center"
          >
            <div className="mx-auto max-w-2xl">
              <h1 id="ai-workspace-title" className="page-title">
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
                className="h-auto min-h-28 justify-start p-5 text-left sm:min-h-32 sm:p-6"
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
                className="h-auto min-h-28 justify-start p-5 text-left sm:min-h-32 sm:p-6"
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

          <DashboardActivity />

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
      </div>

      <NewConsultationPanel
        isOpen={isNewConsultationOpen}
        onClose={() => setIsNewConsultationOpen(false)}
        returnFocusRef={startConsultationButtonRef}
      />
    </>
  );
}

const consultationPageSize = 100;
const chartHeightClasses = [
  "h-1",
  "h-2",
  "h-3",
  "h-5",
  "h-7",
  "h-9",
  "h-11",
  "h-14",
  "h-16",
] as const;

type DailyConsultationActivity = {
  key: string;
  shortLabel: string;
  longLabel: string;
  count: number;
};

function DashboardActivity() {
  const [consultations, setConsultations] = useState<Consultation[]>([]);
  const [retryKey, setRetryKey] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    async function loadConsultations() {
      setIsLoading(true);
      setError(null);

      try {
        const firstPage = await getConsultations({
          page: 0,
          size: consultationPageSize,
        });
        const remainingPages = await loadRemainingConsultationPages(firstPage);

        if (isActive) {
          setConsultations([
            ...firstPage.content,
            ...remainingPages.flatMap((page) => page.content),
          ]);
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

  const activity = useMemo(
    () => createConsultationActivity(consultations),
    [consultations],
  );

  if (isLoading) {
    return <DashboardActivitySkeleton />;
  }

  return (
    <section
      aria-labelledby="activity-title"
      className="mx-auto w-full max-w-4xl border-y border-border bg-background/80 py-6 sm:py-7"
    >
      <div className="flex flex-col gap-6 lg:grid lg:grid-cols-[minmax(0,0.8fr)_minmax(20rem,1.2fr)] lg:gap-10">
        <div>
          <div className="flex items-start justify-between gap-4">
            <div>
              <h2 id="activity-title" className="section-title">
                Activitatea consultațiilor
              </h2>
              <p className="secondary-text mt-1">Rezumat pentru ziua de azi.</p>
            </div>
            <Link
              href="/consultations"
              className="shrink-0 text-sm font-medium text-muted-foreground underline-offset-4 transition-colors hover:text-foreground hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
            >
              Vezi toate
            </Link>
          </div>

          {error ? (
            <div className="mt-5" role="status">
              <p className="secondary-text">{error}</p>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="mt-2 -ml-3"
                onClick={() => setRetryKey((current) => current + 1)}
              >
                Reîncearcă
              </Button>
            </div>
          ) : (
            <dl className="mt-5 grid grid-cols-2 divide-x divide-border">
              <div className="pr-5">
                <dt className="secondary-text">Consultații astăzi</dt>
                <dd className="mt-1 text-2xl font-semibold text-foreground">
                  {activity.todayCount}
                </dd>
              </div>
              <div className="pl-5">
                <dt className="secondary-text">În așteptarea revizuirii</dt>
                <dd className="mt-1 text-2xl font-semibold text-warning">
                  {activity.awaitingReviewCount}
                </dd>
              </div>
            </dl>
          )}
        </div>

        {!error ? <ConsultationActivityChart days={activity.days} /> : null}
      </div>
    </section>
  );
}

async function loadRemainingConsultationPages(
  firstPage: PaginatedResponse<Consultation>,
) {
  if (firstPage.totalPages <= 1) {
    return [];
  }

  return Promise.all(
    Array.from({ length: firstPage.totalPages - 1 }, (_, index) =>
      getConsultations({ page: index + 1, size: consultationPageSize }),
    ),
  );
}

function createConsultationActivity(consultations: Consultation[]) {
  const now = new Date();
  const todayKey = localDateKey(now);
  const days = createLastSevenDays(now);
  const countsByDay = new Map(days.map((day) => [day.key, 0]));

  consultations.forEach((consultation) => {
    const key = localDateKey(new Date(consultation.createdAt));
    if (countsByDay.has(key)) {
      countsByDay.set(key, (countsByDay.get(key) ?? 0) + 1);
    }
  });

  return {
    todayCount: countsByDay.get(todayKey) ?? 0,
    awaitingReviewCount: consultations.filter(
      (consultation) => consultation.status === "NOTES_READY",
    ).length,
    days: days.map((day) => ({
      ...day,
      count: countsByDay.get(day.key) ?? 0,
    })),
  };
}

function createLastSevenDays(now: Date): DailyConsultationActivity[] {
  const formatter = new Intl.DateTimeFormat("ro-RO", { weekday: "short" });
  const longFormatter = new Intl.DateTimeFormat("ro-RO", {
    weekday: "long",
    day: "numeric",
    month: "long",
  });

  return Array.from({ length: 7 }, (_, index) => {
    const date = new Date(now);
    date.setHours(12, 0, 0, 0);
    date.setDate(now.getDate() - (6 - index));

    return {
      key: localDateKey(date),
      shortLabel: formatter.format(date).replace(".", ""),
      longLabel: longFormatter.format(date),
      count: 0,
    };
  });
}

function localDateKey(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function ConsultationActivityChart({
  days,
}: {
  days: DailyConsultationActivity[];
}) {
  const maximumCount = Math.max(...days.map((day) => day.count), 1);
  const totalCount = days.reduce((total, day) => total + day.count, 0);

  if (totalCount === 0) {
    return (
      <div className="flex min-h-28 items-center border-t border-border pt-5 lg:border-l lg:border-t-0 lg:pl-10 lg:pt-0">
        <div>
          <p className="text-sm font-medium text-foreground">
            Nicio consultație în ultimele 7 zile
          </p>
          <p className="secondary-text mt-1 max-w-sm">
            Activitatea va apărea aici după ce creezi prima consultație.
          </p>
        </div>
      </div>
    );
  }

  return (
    <figure className="border-t border-border pt-5 lg:border-l lg:border-t-0 lg:pl-10 lg:pt-0">
      <figcaption className="text-sm font-medium text-foreground">
        Ultimele 7 zile
      </figcaption>
      <div
        className="mt-4 grid h-24 grid-cols-7 items-end gap-2 sm:gap-3"
        role="img"
        aria-label={`Consultații în ultimele șapte zile: ${days
          .map((day) => `${day.longLabel}, ${day.count}`)
          .join("; ")}`}
      >
        {days.map((day) => {
          const level = Math.ceil(
            (day.count / maximumCount) * (chartHeightClasses.length - 1),
          );

          return (
            <div
              key={day.key}
              className="flex h-full min-w-0 flex-col items-center justify-end gap-1.5"
              title={`${day.longLabel}: ${day.count}`}
            >
              <span className="caption-text tabular-nums">{day.count}</span>
              <span
                className={`w-full max-w-8 rounded-sm bg-primary ${chartHeightClasses[level]}`}
                aria-hidden="true"
              />
              <span className="caption-text truncate capitalize">
                {day.shortLabel}
              </span>
            </div>
          );
        })}
      </div>
    </figure>
  );
}

function DashboardActivitySkeleton() {
  return (
    <section
      className="mx-auto w-full max-w-4xl border-y border-border bg-background/80 py-6 sm:py-7"
      aria-label="Se încarcă activitatea consultațiilor"
      aria-busy="true"
    >
      <div className="grid gap-6 lg:grid-cols-2 lg:gap-10">
        <div>
          <Skeleton className="h-5 w-48" />
          <Skeleton className="mt-3 h-4 w-36" />
          <div className="mt-5 grid grid-cols-2 gap-5">
            <Skeleton className="h-14" />
            <Skeleton className="h-14" />
          </div>
        </div>
        <div className="border-t border-border pt-5 lg:border-l lg:border-t-0 lg:pl-10 lg:pt-0">
          <Skeleton className="h-4 w-28" />
          <Skeleton className="mt-4 h-20" />
        </div>
      </div>
    </section>
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

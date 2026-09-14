"use client";

import { useEffect, useMemo, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/ui/empty-state";
import { SearchInput } from "@/components/ui/search-input";
import { Skeleton } from "@/components/ui/skeleton";
import {
  GetMedicationApiError,
  getMedication,
} from "@/features/medication/api/get-medication";
import type { MedicationResponse } from "@/features/medication/types";
import type { PaginatedResponse } from "@/features/consultations/types";

const pageSize = 20;

export function MedicationList() {
  const [medicationsPage, setMedicationsPage] =
    useState<PaginatedResponse<MedicationResponse> | null>(null);
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
  const [retryKey, setRetryKey] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isActive = true;

    async function loadMedications() {
      setIsLoading(true);
      setError(null);

      try {
        const nextPage = await getMedication({ page, size: pageSize });
        if (isActive) {
          setMedicationsPage(nextPage);
        }
      } catch (loadError) {
        if (!isActive) {
          return;
        }
        setError(
          loadError instanceof GetMedicationApiError
            ? loadError.message
            : "Lista medicamentelor nu a putut fi încărcată.",
        );
      } finally {
        if (isActive) {
          setIsLoading(false);
        }
      }
    }

    loadMedications();
    return () => {
      isActive = false;
    };
  }, [page, retryKey]);

  const visibleMedications = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("ro-RO");
    const medications = medicationsPage?.content ?? [];

    if (!normalizedQuery) {
      return medications;
    }

    return medications.filter((medication) =>
      [
        medication.cimCode,
        medication.commercialName,
        medication.activeSubstance,
        medication.atcCode,
        medication.therapeuticAction,
      ]
        .filter(Boolean)
        .join(" ")
        .toLocaleLowerCase("ro-RO")
        .includes(normalizedQuery),
    );
  }, [medicationsPage, query]);

  if (isLoading) {
    return <MedicationListSkeleton />;
  }

  if (error) {
    return (
      <Alert variant="error" title="Nu am putut încărca medicamentele">
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

  if (!medicationsPage || medicationsPage.totalElements === 0) {
    return (
      <EmptyState
        title="Nomenclatorul de medicamente este gol."
        description="Datele din nomenclator vor apărea aici după importare."
      />
    );
  }

  return (
    <div className="space-y-5">
      <div className="sticky top-28 z-20 space-y-2 lg:top-20">
        <label className="block min-w-0">
          <span className="sr-only">Caută în nomenclatorul de medicamente</span>
          <SearchInput
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Denumire, substanță activă, CIM sau cod ATC"
            aria-describedby="medication-search-description"
          />
        </label>
        <p id="medication-search-description" className="caption-text">
          Căutarea se aplică pe pagina încărcată.
        </p>
      </div>

      {medicationsPage.content.length > 0 && visibleMedications.length === 0 ? (
        <EmptyState
          title="Nu am găsit medicamente pentru această căutare."
          description="Încearcă o denumire, o substanță activă, un cod CIM sau un cod ATC diferit."
        />
      ) : null}

      {visibleMedications.length > 0 ? (
        <div className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
          <div className="hidden grid-cols-[minmax(15rem,1.4fr)_minmax(11rem,1fr)_minmax(10rem,.8fr)_minmax(7rem,.55fr)_minmax(7rem,.55fr)_minmax(8rem,.7fr)] gap-4 border-b border-border bg-surface-muted px-4 py-3 text-xs font-semibold uppercase text-muted-foreground lg:grid">
            <span>Medicament</span>
            <span>Substanță activă</span>
            <span>Formă și concentrație</span>
            <span>Cod ATC</span>
            <span>Prescripție</span>
            <span>Actualizat</span>
          </div>
          <ul className="divide-y divide-border">
            {visibleMedications.map((medication) => (
              <MedicationListItem key={medication.cimCode} medication={medication} />
            ))}
          </ul>
        </div>
      ) : null}

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p className="secondary-text">
          Pagina {medicationsPage.number + 1} din {Math.max(medicationsPage.totalPages, 1)} · {medicationsPage.totalElements} medicamente
        </p>
        <div className="grid grid-cols-2 gap-2 sm:flex">
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="w-full sm:w-auto"
            disabled={medicationsPage.first}
            onClick={() => setPage((current) => Math.max(current - 1, 0))}
          >
            Anterioară
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="w-full sm:w-auto"
            disabled={medicationsPage.last}
            onClick={() => setPage((current) => current + 1)}
          >
            Următoare
          </Button>
        </div>
      </div>
    </div>
  );
}

function MedicationListItem({ medication }: { medication: MedicationResponse }) {
  return (
    <li className="grid grid-cols-2 gap-x-4 gap-y-3 px-4 py-5 lg:grid-cols-[minmax(15rem,1.4fr)_minmax(11rem,1fr)_minmax(10rem,.8fr)_minmax(7rem,.55fr)_minmax(7rem,.55fr)_minmax(8rem,.7fr)] lg:items-center">
      <div className="col-span-2 min-w-0 lg:col-span-1">
        <p className="truncate text-sm font-semibold text-foreground" title={medication.commercialName ?? undefined}>
          {medication.commercialName ?? "Denumire indisponibilă"}
        </p>
        <p className="caption-text mt-1 font-mono">CIM {medication.cimCode}</p>
      </div>
      <MedicationField label="Substanță activă" value={medication.activeSubstance} />
      <MedicationField label="Formă și concentrație" value={formatFormAndConcentration(medication)} />
      <MedicationField label="Cod ATC" value={medication.atcCode} mono />
      <MedicationField label="Prescripție" value={medication.prescriptionType} />
      <MedicationField label="Actualizat" value={formatDate(medication.sourceUpdatedAt)} />
    </li>
  );
}

function MedicationField({ label, value, mono = false }: { label: string; value: string | null; mono?: boolean }) {
  return (
    <div className="min-w-0">
      <p className="caption-text lg:hidden">{label}</p>
      <p className={`mt-1 break-words text-sm text-foreground lg:mt-0 lg:truncate ${mono ? "font-mono" : ""}`} title={value ?? undefined}>
        {value ?? "—"}
      </p>
    </div>
  );
}

function formatFormAndConcentration(medication: MedicationResponse) {
  return [medication.pharmaceuticalForm, medication.concentration]
    .filter(Boolean)
    .join(" · ") || null;
}

function formatDate(value: string | null) {
  if (!value) {
    return null;
  }

  return new Intl.DateTimeFormat("ro-RO", { dateStyle: "medium" }).format(
    new Date(`${value}T00:00:00`),
  );
}

function MedicationListSkeleton() {
  return (
    <div className="space-y-4" aria-busy="true">
      <div className="rounded-[var(--radius-surface)] border border-border bg-surface p-4 shadow-surface">
        <Skeleton className="h-4 w-40" />
        <Skeleton className="mt-3 h-[var(--control-height)] max-w-xl" />
      </div>
      <div className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
        <div className="divide-y divide-border">
          {Array.from({ length: 5 }, (_, index) => (
            <div key={index} className="grid gap-3 px-4 py-4 lg:grid-cols-6">
              {Array.from({ length: 6 }, (_, cellIndex) => (
                <Skeleton key={cellIndex} className="h-10" />
              ))}
            </div>
          ))}
        </div>
        <p className="secondary-text px-4 py-3">Se încarcă medicamentele...</p>
      </div>
    </div>
  );
}

"use client";

import { useEffect, useMemo, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  GetPatientsApiError,
  getPatients,
} from "@/features/patients/api/get-patients";
import type { Patient, PatientSex } from "@/features/patients/types";

type PatientPickerProps = {
  onSelectPatient: (patient: Patient) => void;
  onCreatePatient: () => void;
};

const sexLabels: Record<PatientSex, string> = {
  FEMALE: "Feminin",
  MALE: "Masculin",
  OTHER: "Altul",
  UNKNOWN: "Necunoscut",
};

export default function PatientPicker({
  onSelectPatient,
  onCreatePatient,
}: PatientPickerProps) {
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
  }, [retryKey]);

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
    return (
      <Card variant="standard" className="p-5">
        <div className="space-y-3" aria-busy="true">
          <div className="h-4 w-40 rounded-[var(--radius-control)] bg-secondary" />
          <div className="h-12 rounded-[var(--radius-control)] bg-surface-muted" />
          <div className="h-12 rounded-[var(--radius-control)] bg-surface-muted" />
          <p className="secondary-text">Se încarcă pacienții...</p>
        </div>
      </Card>
    );
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
      <label className="block space-y-2">
        <span className="text-sm font-medium text-foreground">
          Caută pacient
        </span>
        <Input
          type="search"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Nume, telefon sau email"
          aria-label="Caută pacient în lista încărcată"
        />
      </label>

      {patients.length === 0 ? (
        <Card variant="muted" className="p-5">
          <p className="text-sm font-medium text-foreground">
            Nu există pacienți încă.
          </p>
          <p className="secondary-text mt-2">
            Adaugă primul pacient pentru a începe consultația.
          </p>
          <Button
            type="button"
            variant="primary"
            className="mt-4"
            onClick={onCreatePatient}
          >
            Adaugă pacient
          </Button>
        </Card>
      ) : null}

      {patients.length > 0 && visiblePatients.length === 0 ? (
        <Card variant="muted" className="p-5">
          <p className="text-sm font-medium text-foreground">
            Nu am găsit pacienți pentru această căutare.
          </p>
          <p className="secondary-text mt-1">
            Verifică numele, telefonul sau adresa de email.
          </p>
        </Card>
      ) : null}

      {visiblePatients.length > 0 ? (
        <div className="overflow-hidden rounded-[var(--radius-surface)] border border-border bg-surface shadow-surface">
          <ul className="divide-y divide-border">
            {visiblePatients.map((patient) => (
              <PatientPickerItem
                key={patient.id}
                patient={patient}
                onSelectPatient={onSelectPatient}
              />
            ))}
          </ul>
        </div>
      ) : null}
    </div>
  );
}

function PatientPickerItem({
  patient,
  onSelectPatient,
}: {
  patient: Patient;
  onSelectPatient: (patient: Patient) => void;
}) {
  return (
    <li className="grid gap-3 px-4 py-4 transition-colors hover:bg-surface-muted sm:grid-cols-[minmax(0,1fr)_auto] sm:items-center">
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <p className="truncate text-sm font-semibold text-foreground">
            {patient.lastName} {patient.firstName}
          </p>
          <Badge variant={patient.status === "ACTIVE" ? "success" : "neutral"}>
            {patient.status === "ACTIVE" ? "Activ" : "Arhivat"}
          </Badge>
        </div>
        <dl className="mt-2 flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted-foreground">
          <PatientMeta label="Data nașterii" value={patient.birthDate} />
          <PatientMeta
            label="Sex"
            value={patient.sex ? sexLabels[patient.sex] : null}
          />
          <PatientMeta label="Telefon" value={patient.phone} />
          <PatientMeta label="Email" value={patient.email} />
        </dl>
      </div>
      <Button
        type="button"
        variant="primary"
        size="sm"
        onClick={() => onSelectPatient(patient)}
      >
        Începe consultația
      </Button>
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

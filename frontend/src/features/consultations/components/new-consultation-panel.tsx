"use client";

import { useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CreatePatientForm } from "@/features/patients/components/create-patient-form";
import PatientPicker from "@/features/patients/components/patient-picker";
import type { Patient } from "@/features/patients/types";

type NewConsultationPanelProps = {
  isOpen: boolean;
  onClose: () => void;
  onPatientCreated?: (patient: Patient) => void;
};

type PanelStep = "search" | "create" | "created";

export function NewConsultationPanel({
  isOpen,
  onClose,
  onPatientCreated,
}: NewConsultationPanelProps) {
  const [step, setStep] = useState<PanelStep>("search");
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);

  if (!isOpen) {
    return null;
  }

  function handleClose() {
    setStep("search");
    setSelectedPatient(null);
    onClose();
  }

  function handleCreated(patient: Patient) {
    setSelectedPatient(patient);
    onPatientCreated?.(patient);
    setStep("created");
  }

  function handleSelectPatient(patient: Patient) {
    setSelectedPatient(patient);
    setStep("created");

  }

  return (
    <div
      className="fixed inset-0 z-50 bg-foreground/35"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          handleClose();
        }
      }}
    >
      <aside
        className="ml-auto flex h-full w-full max-w-2xl flex-col border-l border-border bg-surface shadow-elevated"
        role="dialog"
        aria-modal="true"
        aria-labelledby="new-consultation-title"
      >
        <header className="border-b border-border px-5 py-4 sm:px-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <Badge variant={step === "created" ? "success" : "info"}>
                {step === "created" ? "Pacient selectat" : "În pregătire"}
              </Badge>
              <h2 id="new-consultation-title" className="mt-3 text-xl font-semibold text-foreground">
                Consultație nouă
              </h2>
              <p className="secondary-text mt-1">
                Alege pacientul pentru care începi consultația.
              </p>
            </div>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={handleClose}
              aria-label="Închide panoul pentru consultație nouă"
            >
              Închide
            </Button>
          </div>
        </header>

        {step === "search" ? (
          <SearchPatientStep
            onCreatePatient={() => setStep("create")}
            onSelectPatient={handleSelectPatient}
          />
        ) : null}

        {step === "create" ? (
          <CreatePatientForm
            onCancel={() => setStep("search")}
            onCreated={handleCreated}
          />
        ) : null}

        {step === "created" && selectedPatient ? (
          <CreatedPatientStep patient={selectedPatient} onClose={handleClose} />
        ) : null}
      </aside>
    </div>
  );
}

function SearchPatientStep({
  onCreatePatient,
  onSelectPatient,
}: {
  onCreatePatient: () => void;
  onSelectPatient: (patient: Patient) => void;
}) {
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5 sm:px-6">
        <div className="space-y-5">
          <section aria-labelledby="patient-search-title">
            <div className="mb-4">
              <h3 id="patient-search-title" className="section-title">
                Alege pacientul
              </h3>
              <p className="secondary-text mt-1">
                Caută un pacient existent sau adaugă unul nou.
              </p>
            </div>

            <PatientPicker
              onSelectPatient={onSelectPatient}
              onCreatePatient={onCreatePatient}
            />
          </section>
        </div>
      </div>


      <div className="border-t border-border bg-surface px-5 py-4 sm:px-6">
        <div className="flex justify-end">
          <Button type="button" variant="outline" onClick={onCreatePatient}>
            Continuă cu pacient nou
          </Button>
        </div>
      </div>
    </div>
  );
}

function CreatedPatientStep({
  patient,
  onClose,
}: {
  patient: Patient;
  onClose: () => void;
}) {
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5 sm:px-6">
        <Alert variant="success" title="Pacient adăugat">
          Pacientul este pregătit pentru consultație. Crearea consultației va fi
          conectată după ce endpointul dedicat este disponibil.
        </Alert>

        <section className="mt-5" aria-labelledby="selected-patient-title">
          <h3 id="selected-patient-title" className="section-title">
            Pacient selectat
          </h3>
          <div className="mt-3 rounded-[var(--radius-control)] border border-border bg-surface-muted p-4">
            <p className="text-base font-semibold text-foreground">
              {patient.firstName} {patient.lastName}
            </p>
            <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2">
              <div>
                <dt className="caption-text">Data nașterii</dt>
                <dd className="mt-1 text-foreground">
                  {patient.birthDate || "Nespecificată"}
                </dd>
              </div>
              <div>
                <dt className="caption-text">Status</dt>
                <dd className="mt-1">
                  <Badge variant="success">Activ</Badge>
                </dd>
              </div>
              <div>
                <dt className="caption-text">Telefon</dt>
                <dd className="mt-1 text-foreground">
                  {patient.phone || "Nespecificat"}
                </dd>
              </div>
              <div>
                <dt className="caption-text">Email</dt>
                <dd className="mt-1 text-foreground">
                  {patient.email || "Nespecificat"}
                </dd>
              </div>
            </dl>
          </div>
        </section>
      </div>

      <div className="border-t border-border bg-surface px-5 py-4 sm:px-6">
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="button" variant="outline" onClick={onClose}>
            Închide
          </Button>
          <Button type="button" variant="primary" disabled>
            Continuă la consultație
          </Button>
        </div>
      </div>
    </div>
  );
}

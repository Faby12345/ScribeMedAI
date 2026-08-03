"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  CreateConsultationApiError,
  createConsultation,
} from "@/features/consultations/api/create-consultation";
import { CreatePatientForm } from "@/features/patients/components/create-patient-form";
import PatientPicker from "@/features/patients/components/patient-picker";
import type { Patient } from "@/features/patients/types";

type NewConsultationPanelProps = {
  isOpen: boolean;
  onClose: () => void;
  onPatientCreated?: (patient: Patient) => void;
};

type PanelStep = "search" | "create";
const panelExitDurationMs = 180;

export function NewConsultationPanel({
  isOpen,
  onClose,
  onPatientCreated,
}: NewConsultationPanelProps) {
  const router = useRouter();
  const [shouldRender, setShouldRender] = useState(isOpen);
  const [isClosing, setIsClosing] = useState(false);
  const [step, setStep] = useState<PanelStep>("search");
  const [pendingPatient, setPendingPatient] = useState<Patient | null>(null);
  const [isCreatingConsultation, setIsCreatingConsultation] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setShouldRender(true);
      setIsClosing(false);
      return;
    }

    if (!shouldRender) {
      return;
    }

    setIsClosing(true);
    const timeoutId = window.setTimeout(() => {
      setShouldRender(false);
      setIsClosing(false);
      setStep("search");
      setPendingPatient(null);
      setSubmitError(null);
    }, panelExitDurationMs);

    return () => window.clearTimeout(timeoutId);
  }, [isOpen, shouldRender]);

  if (!shouldRender) {
    return null;
  }

  function handleClose() {
    if (isCreatingConsultation) {
      return;
    }

    onClose();
  }

  function handleCreated(patient: Patient) {
    onPatientCreated?.(patient);
    setStep("search");
    setPendingPatient(patient);
    setSubmitError(null);
  }

  function handleSelectPatient(patient: Patient) {
    setPendingPatient(patient);
    setSubmitError(null);
  }

  async function startConsultation() {
    if (!pendingPatient || isCreatingConsultation) {
      return;
    }

    setSubmitError(null);
    setIsCreatingConsultation(true);

    try {
      const consultation = await createConsultation(pendingPatient.id);
      router.push(`/consultations/${consultation.id}`);
      router.refresh();
    } catch (error) {
      if (error instanceof CreateConsultationApiError) {
        setSubmitError(error.message);
      } else {
        setSubmitError(
          "Consultația nu a putut fi creată. Încearcă din nou.",
        );
      }
      setIsCreatingConsultation(false);
    }
  }

  return (
    <div
      className={`fixed inset-0 z-50 bg-foreground/35 ${
        isClosing ? "panel-overlay-exit" : "panel-overlay-enter"
      }`}
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          handleClose();
        }
      }}
    >
      <aside
        className={`ml-auto flex h-full w-full max-w-2xl flex-col border-l border-border bg-surface shadow-elevated ${
          isClosing ? "side-panel-exit" : "side-panel-enter"
        }`}
        role="dialog"
        aria-modal="true"
        aria-labelledby="new-consultation-title"
      >
        <header className="border-b border-border px-5 py-4 sm:px-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <Badge variant={isCreatingConsultation ? "processing" : "info"}>
                {isCreatingConsultation ? "Se creează" : "În pregătire"}
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
              disabled={isCreatingConsultation}
              aria-label="Închide panoul pentru consultație nouă"
            >
              Închide
            </Button>
          </div>
        </header>

        {submitError ? (
          <div className="border-b border-border px-5 py-4 sm:px-6">
            <Alert variant="error" title="Consultația nu a fost creată">
              {submitError}
            </Alert>
          </div>
        ) : null}

        {step === "search" ? (
          <SearchPatientStep
            onCreatePatient={() => setStep("create")}
            onSelectPatient={handleSelectPatient}
            isCreatingConsultation={isCreatingConsultation}
          />
        ) : null}

        {step === "create" ? (
          <CreatePatientForm
            onCancel={() => setStep("search")}
            onCreated={handleCreated}
          />
        ) : null}

        {pendingPatient ? (
          <ConfirmConsultationDialog
            patient={pendingPatient}
            isCreating={isCreatingConsultation}
            onCancel={() => setPendingPatient(null)}
            onConfirm={() => void startConsultation()}
          />
        ) : null}
      </aside>
    </div>
  );
}

function SearchPatientStep({
  onCreatePatient,
  onSelectPatient,
  isCreatingConsultation,
}: {
  onCreatePatient: () => void;
  onSelectPatient: (patient: Patient) => void;
  isCreatingConsultation: boolean;
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
          <Button
            type="button"
            variant="outline"
            onClick={onCreatePatient}
            disabled={isCreatingConsultation}
          >
            Continuă cu pacient nou
          </Button>
        </div>
      </div>
    </div>
  );
}

function ConfirmConsultationDialog({
  patient,
  isCreating,
  onCancel,
  onConfirm,
}: {
  patient: Patient;
  isCreating: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}) {
  return (
    <div
      className="absolute inset-0 z-10 flex items-center justify-center bg-foreground/30 px-4"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isCreating) {
          onCancel();
        }
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-consultation-title"
        className="w-full max-w-md rounded-[var(--radius-surface)] border border-border bg-surface p-5 shadow-elevated"
      >
        <Badge variant="warning">Confirmare necesară</Badge>
        <h3
          id="confirm-consultation-title"
          className="mt-3 text-lg font-semibold text-foreground"
        >
          Creezi o consultație nouă?
        </h3>
        <p className="secondary-text mt-2">
          Verifică pacientul selectat înainte de a deschide fișa consultației.
        </p>

        <div className="mt-4 rounded-[var(--radius-control)] border border-border bg-surface-muted p-4">
          <p className="text-sm font-semibold text-foreground">
            {patient.lastName} {patient.firstName}
          </p>
          <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2">
            <div>
              <dt className="caption-text">Data nașterii</dt>
              <dd className="mt-1 text-foreground">
                {patient.birthDate || "Nespecificată"}
              </dd>
            </div>
            <div>
              <dt className="caption-text">Telefon</dt>
              <dd className="mt-1 text-foreground">
                {patient.phone || "Nespecificat"}
              </dd>
            </div>
          </dl>
        </div>

        <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={isCreating}
          >
            Anulează
          </Button>
          <Button
            type="button"
            variant="primary"
            onClick={onConfirm}
            isLoading={isCreating}
            loadingText="Se creează"
          >
            Creează consultația
          </Button>
        </div>
      </section>
    </div>
  );
}

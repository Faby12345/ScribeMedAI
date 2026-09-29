"use client";

import { useRouter } from "next/navigation";
import {
  type KeyboardEvent,
  type RefObject,
  useEffect,
  useRef,
  useState,
} from "react";

import { Alert } from "@/components/ui/alert";
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
  returnFocusRef?: RefObject<HTMLElement | null>;
};

type PanelStep = "search" | "create";
const focusableSelector = [
  "a[href]",
  "button:not([disabled])",
  "input:not([disabled])",
  "select:not([disabled])",
  "textarea:not([disabled])",
  '[tabindex]:not([tabindex="-1"])',
].join(",");

export function NewConsultationPanel({
  isOpen,
  onClose,
  onPatientCreated,
  returnFocusRef,
}: NewConsultationPanelProps) {
  const router = useRouter();
  const panelRef = useRef<HTMLElement>(null);
  const closeButtonRef = useRef<HTMLButtonElement>(null);
  const [step, setStep] = useState<PanelStep>("search");
  const [pendingPatient, setPendingPatient] = useState<Patient | null>(null);
  const [isCreatingConsultation, setIsCreatingConsultation] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const focusReturnTarget =
      returnFocusRef?.current ??
      (document.activeElement instanceof HTMLElement
        ? document.activeElement
        : null);

    const animationFrameId = window.requestAnimationFrame(() => {
      panelRef.current?.focus();
    });

    return () => {
      window.cancelAnimationFrame(animationFrameId);
      focusReturnTarget?.focus();
      setStep("search");
      setPendingPatient(null);
      setSubmitError(null);
    };
  }, [isOpen, returnFocusRef]);

  if (!isOpen) {
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

  function handlePanelKeyDown(event: KeyboardEvent<HTMLElement>) {
    if (event.key === "Escape") {
      event.preventDefault();
      handleClose();
      return;
    }

    if (event.key !== "Tab" || pendingPatient) {
      return;
    }

    trapFocus(event, panelRef.current);
  }

  return (
    <div
      className="fixed inset-0 z-50 bg-foreground/35 panel-overlay-enter"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          handleClose();
        }
      }}
    >
      <aside
        ref={panelRef}
        className="ml-auto flex h-full w-full max-w-2xl flex-col border-l border-border bg-surface shadow-elevated side-panel-enter"
        role="dialog"
        aria-modal="true"
        aria-labelledby="new-consultation-title"
        aria-describedby="new-consultation-description"
        tabIndex={-1}
        onKeyDown={handlePanelKeyDown}
      >
        <header className="border-b border-border px-5 py-4 sm:px-6">
          <div className="flex items-start justify-between gap-4">
            <div>

              <h2 id="new-consultation-title" className="mt-3 text-xl font-semibold text-foreground">
                Consultație nouă
              </h2>
              <p id="new-consultation-description" className="secondary-text mt-1">
                Alege pacientul pentru care începi consultația.
              </p>
            </div>
            <Button
              ref={closeButtonRef}
              type="button"
              variant="ghost"
              size="icon"
              className="bg-transparent text-muted-foreground shadow-none hover:border-border hover:bg-surface hover:text-foreground active:bg-surface-muted"
              onClick={handleClose}
              disabled={isCreatingConsultation}
              aria-label="Închide panoul pentru consultație nouă"
            >
              <svg
                aria-hidden="true"
                className="size-4"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M18 6 6 18" />
                <path d="m6 6 12 12" />
              </svg>
              <span className="sr-only">Închide</span>
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

function trapFocus(
  event: KeyboardEvent<HTMLElement>,
  container: HTMLElement | null,
) {
  if (!container) {
    return;
  }

  const focusableElements = Array.from(
    container.querySelectorAll<HTMLElement>(focusableSelector),
  ).filter(
    (element) =>
      !element.hasAttribute("disabled") &&
      element.getAttribute("aria-hidden") !== "true" &&
      element.offsetParent !== null,
  );

  if (focusableElements.length === 0) {
    event.preventDefault();
    container.focus();
    return;
  }

  const firstElement = focusableElements[0];
  const lastElement = focusableElements[focusableElements.length - 1];

  if (event.shiftKey && document.activeElement === firstElement) {
    event.preventDefault();
    lastElement.focus();
    return;
  }

  if (!event.shiftKey && document.activeElement === lastElement) {
    event.preventDefault();
    firstElement.focus();
  }
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
            </div>

            <PatientPicker
              onSelectPatient={onSelectPatient}
              onCreatePatient={onCreatePatient}
              autoFocusSearch
            />
          </section>
        </div>
      </div>


      <div className="border-t border-border bg-surface px-5 py-4 sm:px-6">
        <div className="flex justify-end">
          <Button
            type="button"
            variant="success"
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
  const dialogRef = useRef<HTMLElement>(null);
  const cancelButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    cancelButtonRef.current?.focus();
  }, []);

  function handleConfirmKeyDown(event: KeyboardEvent<HTMLElement>) {
    if (event.key === "Escape") {
      event.preventDefault();
      event.stopPropagation();

      if (!isCreating) {
        onCancel();
      }

      return;
    }

    if (event.key === "Tab") {
      event.stopPropagation();
      trapFocus(event, dialogRef.current);
    }
  }

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
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-consultation-title"
        aria-describedby="confirm-consultation-description confirm-consultation-patient"
        className="w-full max-w-md rounded-[var(--radius-surface)] border border-border bg-surface p-5 shadow-elevated"
        tabIndex={-1}
        onKeyDown={handleConfirmKeyDown}
      >
        <h3
          id="confirm-consultation-title"
          className="mt-3 text-lg font-semibold text-foreground"
        >
          Creezi o consultație nouă?
        </h3>
        <p id="confirm-consultation-description" className="secondary-text mt-2">
          Verifică pacientul selectat înainte de a deschide fișa consultației.
        </p>

        <div
          id="confirm-consultation-patient"
          className="mt-4 rounded-[var(--radius-control)] border border-border bg-surface-muted p-4"
        >
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
            ref={cancelButtonRef}
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

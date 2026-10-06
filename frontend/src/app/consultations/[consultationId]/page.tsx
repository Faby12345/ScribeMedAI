import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { ButtonLink } from "@/components/ui/button";
import {
  GetConsultationApiError,
  getConsultation,
} from "@/features/consultations/api/get-consultation";
import { ConsultationDocumentationFlow } from "@/features/consultations/components/consultation-documentation-flow";
import type {
  Consultation,
  ConsultationStatus,
} from "@/features/consultations/types";
import { getCurrentUser } from "@/features/auth/api/current-user";
import { getPatientById } from "@/features/patients/api/get-patient-server";
import { PatientAvatar } from "@/features/patients/components/patient-avatar";
import type { Patient } from "@/features/patients/types";
import { cn } from "@/lib/class-names";

type ConsultationPageProps = {
  params: Promise<{
    consultationId: string;
  }>;
};

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

const statusLabels: Record<ConsultationStatus, string> = {
  CREATED: "Creată",
  PATIENT_INFORMED: "Pacient informat",
  AUDIO_UPLOADED: "Audio încărcat",
  TRANSCRIBING: "În transcriere",
  TRANSCRIPTION_READY: "Transcriere disponibilă",
  TRANSCRIPTION_FAILED: "Transcriere eșuată",
  NOTES_PROCESSING: "Draft în generare",
  NOTES_READY: "De revizuit",
  NOTES_FAILED: "Generare draft eșuată",
  APPROVED: "Finalizată",
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
  NOTES_READY: "warning",
  NOTES_FAILED: "destructive",
  APPROVED: "success",
};

export default async function ConsultationPage({
  params,
}: ConsultationPageProps) {
  const { consultationId } = await params;

  if (!uuidPattern.test(consultationId)) {
    notFound();
  }

  const requestCookies = await cookies();
  const user = await getCurrentUser(requestCookies);

  if (!user) {
    redirect("/login");
  }

  let consultation: Consultation;

  try {
    consultation = await getConsultation(consultationId, requestCookies);
  } catch (error) {
    if (
      error instanceof GetConsultationApiError &&
      (error.status === 401 || error.status === 403)
    ) {
      redirect("/login");
    }

    if (error instanceof GetConsultationApiError && error.status === 404) {
      notFound();
    }

    throw error;
  }

  const patient = await getPatientById(
    consultation.patientId,
    requestCookies,
  ).catch(() => null);
  const patientName = patient
    ? `${patient.lastName} ${patient.firstName}`.trim()
    : "Consultație";
  const isReady = consultation.status === "NOTES_READY";
  const isApproved = consultation.status === "APPROVED";
  const isProcessing = [
    "AUDIO_UPLOADED",
    "TRANSCRIBING",
    "TRANSCRIPTION_READY",
    "NOTES_PROCESSING",
  ].includes(consultation.status);
  const hasFailed = ["TRANSCRIPTION_FAILED", "NOTES_FAILED"].includes(
    consultation.status,
  );

  return (
    <div className="mx-auto w-full max-w-[82rem] px-5 py-6 sm:px-8 sm:py-8 lg:px-10">
      <header className="border-b border-border pb-5">
        <ButtonLink href="/dashboard" variant="ghost" size="sm" className="-ml-3 mb-3">
          <BackIcon />
          Înapoi la panou
        </ButtonLink>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div className="min-w-0">
            <div className="mb-2 flex flex-wrap items-center gap-2">
              <Badge variant={statusVariants[consultation.status]}>
                {statusLabels[consultation.status]}
              </Badge>
              <span className="caption-text">Consultație clinică</span>
            </div>
            <h1 className="page-title truncate">{patientName}</h1>
            <p className="secondary-text mt-1">
              Documentează consultația și verifică rezultatul înainte de aprobare.
            </p>
          </div>
          {isReady || isApproved ? (
            <ButtonLink href={`/consultations/${consultation.id}/review`}>
              {isApproved ? "Vezi documentul" : "Revizuiește documentul"}
              <ForwardIcon />
            </ButtonLink>
          ) : null}
        </div>
      </header>

      <PatientContext
        patient={patient}
        consultation={consultation}
      />

      <div className="mx-auto mt-8 w-full max-w-5xl">
        {isReady || isApproved ? (
          <ReadyState
            consultationId={consultation.id}
            isApproved={isApproved}
          />
        ) : isProcessing ? (
          <ProcessingState
            consultationId={consultation.id}
            status={consultation.status}
          />
        ) : (
          <>
            {hasFailed ? (
              <Alert variant="error" title="Procesarea nu a fost finalizată" className="mb-6">
                Poți relua documentarea mai jos sau poți încerca din nou cu un alt fișier audio.
              </Alert>
            ) : null}
            <ConsultationDocumentationFlow
              consultationId={consultationId}
              isPatientInformed={Boolean(consultation.patientInformedAt)}
            />
          </>
        )}
      </div>
    </div>
  );
}

function PatientContext({
  patient,
  consultation,
}: {
  patient: Patient | null;
  consultation: Consultation;
}) {
  return (
    <section aria-label="Context pacient și consultație" className="border-b border-border py-4">
      <div className="grid grid-cols-2 gap-x-5 gap-y-4 lg:grid-cols-4 xl:grid-cols-[minmax(13rem,1.3fr)_repeat(6,minmax(7rem,1fr))] xl:items-center">
        {patient ? (
          <div className="col-span-2 flex min-w-0 items-center gap-3 xl:col-span-1">
            <PatientAvatar patient={patient} className="size-10" />
            <div className="min-w-0">
              <p className="caption-text">Pacient</p>
              <p className="truncate text-sm font-semibold text-foreground">
                {patient.lastName} {patient.firstName}
              </p>
            </div>
          </div>
        ) : (
          <p className="col-span-2 text-sm text-warning xl:col-span-1">
            Datele pacientului nu au putut fi încărcate.
          </p>
        )}
        <dl className="contents">
          <DetailItem label="Data nașterii" value={patient?.birthDate ?? "Nespecificată"} />
          <DetailItem label="Telefon" value={patient?.phone ?? "Nespecificat"} />
          <DetailItem label="Email" value={patient?.email ?? "Nespecificat"} />
          <DetailItem label="Creată" value={formatDateTime(consultation.createdAt)} />
          <DetailItem label="Actualizată" value={formatDateTime(consultation.updatedAt)} />
          <DetailItem
            label="Informare audio"
            value={consultation.patientInformedAt ? "Confirmată" : "Neconfirmată"}
          />
        </dl>
      </div>
    </section>
  );
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 xl:border-l xl:border-border xl:pl-4">
      <dt className="caption-text">{label}</dt>
      <dd className="mt-1 truncate text-sm text-foreground">{value}</dd>
    </div>
  );
}

function ProcessingState({
  consultationId,
  status,
}: {
  consultationId: string;
  status: ConsultationStatus;
}) {
  const activeStep = status === "AUDIO_UPLOADED" || status === "TRANSCRIBING"
    ? 1
    : status === "TRANSCRIPTION_READY" || status === "NOTES_PROCESSING"
      ? 2
      : 0;
  const steps = ["Audio primit", "Transcriere", "Document clinic"];

  return (
    <section aria-labelledby="processing-title" className="py-8 sm:py-12">
      <div className="mx-auto max-w-2xl text-center">
        <span className="mx-auto flex size-12 items-center justify-center rounded-full bg-primary-soft text-primary">
          <ProcessingIcon />
        </span>
        <h2 id="processing-title" className="mt-5 text-xl font-semibold text-foreground">
          Documentul este în pregătire
        </h2>
        <p className="secondary-text mx-auto mt-2 max-w-xl">
          Procesarea continuă în fundal. Poți reveni în panou și deschide consultația mai târziu.
        </p>
      </div>

      <ol className="mx-auto mt-8 grid max-w-3xl grid-cols-3" aria-label="Progres procesare">
        {steps.map((label, index) => {
          const isComplete = index < activeStep;
          const isActive = index === activeStep;
          return (
            <li key={label} className="relative text-center">
              {index > 0 ? (
                <span className={cn(
                  "absolute right-1/2 top-3.5 h-px w-full",
                  isComplete || isActive ? "bg-primary" : "bg-border",
                )} aria-hidden="true" />
              ) : null}
              <span className={cn(
                "relative mx-auto flex size-7 items-center justify-center rounded-full border text-xs font-semibold",
                isComplete
                  ? "border-primary bg-primary text-primary-foreground"
                  : isActive
                    ? "border-primary bg-surface text-primary"
                    : "border-border bg-surface text-muted-foreground",
              )}>
                {isComplete ? <CheckIcon /> : index + 1}
              </span>
              <span className={cn(
                "mt-2 block text-xs sm:text-sm",
                isActive ? "font-semibold text-foreground" : "text-muted-foreground",
              )}>
                {label}
              </span>
            </li>
          );
        })}
      </ol>

      <div className="mt-9 flex flex-col justify-center gap-2 sm:flex-row">
        <ButtonLink href={`/consultations/${consultationId}`}>
          Actualizează starea
        </ButtonLink>
        <ButtonLink href="/dashboard" variant="outline">
          Revino la panou
        </ButtonLink>
      </div>
    </section>
  );
}

function ReadyState({
  consultationId,
  isApproved,
}: {
  consultationId: string;
  isApproved: boolean;
}) {
  return (
    <section aria-labelledby="ready-title" className="py-8 sm:py-12">
      <div className="mx-auto max-w-2xl text-center">
        <span className="mx-auto flex size-12 items-center justify-center rounded-full bg-success-soft text-success">
          <CheckIcon className="size-6" />
        </span>
        <h2 id="ready-title" className="mt-5 text-xl font-semibold text-foreground">
          {isApproved
            ? "Consultația este finalizată"
            : "Documentul este pregătit pentru revizuire"}
        </h2>
        <p className="secondary-text mx-auto mt-2 max-w-xl">
          {isApproved
            ? "Documentul clinic a fost verificat și aprobat de medic. Îl poți consulta mai jos."
            : "Verifică fiecare secțiune și aprobă documentul numai după ce conținutul clinic este corect."}
        </p>
        <div className="mt-7 flex flex-col justify-center gap-2 sm:flex-row">
          <ButtonLink href={`/consultations/${consultationId}/review`}>
            {isApproved ? "Vezi documentul final" : "Deschide documentul"}
            <ForwardIcon />
          </ButtonLink>
          <ButtonLink href="/dashboard" variant="outline">
            Revino la panou
          </ButtonLink>
        </div>
      </div>
    </section>
  );
}

function BackIcon() {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="m15 18-6-6 6-6" /></svg>;
}

function ForwardIcon() {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="m9 18 6-6-6-6" /></svg>;
}

function CheckIcon({ className = "size-3.5" }: { className?: string }) {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className={className} stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m5 12 4 4L19 6" /></svg>;
}

function ProcessingIcon() {
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" className="size-6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"><path d="M12 3a9 9 0 1 0 9 9" /><path d="M12 7v5l3 2" /></svg>;
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

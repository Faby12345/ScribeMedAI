import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { ButtonLink } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
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

  return (
    <div className="page-container py-7 sm:py-8">
      <div className="mx-auto w-full max-w-6xl">
        <PageHeader
          title={patientName}
          actions={
            <>
              {consultation.status === "NOTES_READY" ? (
                <ButtonLink href={`/consultations/${consultation.id}/review`}>
                  Revizuiește draftul
                </ButtonLink>
              ) : null}
              <Badge variant={statusVariants[consultation.status]}>
                {statusLabels[consultation.status]}
              </Badge>
              <ButtonLink href="/dashboard" variant="outline">
                Înapoi la panou
              </ButtonLink>
            </>
          }
        />

        <div className="grid min-w-0 gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(17rem,20rem)]">
          <section className="min-w-0">
            <ConsultationDocumentationFlow
              consultationId={consultationId}
              isPatientInformed={Boolean(consultation.patientInformedAt)}
            />
          </section>

          <aside className="min-w-0 space-y-4">
            <PatientSummary patient={patient} />
            <Card>
              <CardContent className="p-5">
                <h2 className="section-title">Consultație</h2>
                <dl className="mt-4 space-y-3">
                  <DetailItem
                    label="Creată"
                    value={formatDateTime(consultation.createdAt)}
                  />
                  <DetailItem
                    label="Actualizată"
                    value={formatDateTime(consultation.updatedAt)}
                  />
                  <DetailItem
                    label="Informare pacient"
                    value={
                      consultation.patientInformedAt
                        ? formatDateTime(consultation.patientInformedAt)
                        : "Neconfirmată"
                    }
                  />
                </dl>
              </CardContent>
            </Card>
          </aside>
        </div>
      </div>
    </div>
  );
}

function PatientSummary({
  patient,
}: {
  patient: Patient | null;
}) {
  if (!patient) {
    return (
      <Alert variant="warning" title="Date pacient indisponibile">
        Pacientul asociat nu a putut fi încărcat. Revino la lista de pacienți
        pentru verificare.
      </Alert>
    );
  }

  return (
    <Card>
      <CardContent className="p-5">
        <div className="flex items-center gap-3">
          <PatientAvatar patient={patient} className="size-9" />
          <div className="min-w-0">
            <h2 className="section-title">Pacient</h2>
            <p className="mt-1 truncate text-sm font-semibold text-foreground">
              {patient.lastName} {patient.firstName}
            </p>
          </div>
        </div>
        <dl className="mt-4 space-y-3">
          <DetailItem
            label="Data nașterii"
            value={patient.birthDate ?? "Nespecificată"}
          />
          <DetailItem label="Telefon" value={patient.phone ?? "Nespecificat"} />
          <DetailItem label="Email" value={patient.email ?? "Nespecificat"} />
        </dl>
      </CardContent>
    </Card>
  );
}

function DetailItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="caption-text">{label}</dt>
      <dd className="mt-1 break-words text-sm text-foreground">{value}</dd>
    </div>
  );
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

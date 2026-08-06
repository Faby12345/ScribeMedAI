import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { ButtonLink } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
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

  return (
    <div className="page-container py-7 sm:py-8">
      <PageHeader
        title="Consultație"
        description="Verifică datele consultației și continuă cu pașii clinici necesari pentru documentare."
        actions={
          <>
            <Badge variant={statusVariants[consultation.status]}>
              {statusLabels[consultation.status]}
            </Badge>
            <ButtonLink href="/dashboard" variant="outline">
              Înapoi la panou
            </ButtonLink>
          </>
        }
      />

        <div className="grid min-w-0 gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(18rem,22rem)]">
          <section className="min-w-0 space-y-6">
            <Card>
              <CardHeader>
                <h2 className="section-title">Detalii consultație</h2>
                <p className="secondary-text">
                  Starea curentă și pașii disponibili pentru fluxul de lucru.
                </p>
              </CardHeader>
              <CardContent>
                <dl className="grid gap-4 sm:grid-cols-2">
                  <DetailItem
                    label="Creată la"
                    value={formatDateTime(consultation.createdAt)}
                  />
                  <DetailItem
                    label="Actualizată la"
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
                  <div>
                    <dt className="caption-text">Status</dt>
                    <dd className="mt-1">
                      <Badge variant={statusVariants[consultation.status]}>
                        {statusLabels[consultation.status]}
                      </Badge>
                    </dd>
                  </div>
                </dl>
              </CardContent>
            </Card>

            <NextStep consultation={consultation} />
            <ConsultationDocumentationFlow
              isPatientInformed={Boolean(consultation.patientInformedAt)}
            />
          </section>

          <aside className="min-w-0 space-y-6">
            <PatientSummary patient={patient} />
            <Card variant="muted">
              <CardHeader>
                <h2 className="section-title">Medic</h2>
              </CardHeader>
              <CardContent>
                <p className="text-sm font-medium text-foreground">
                  {user.displayName}
                </p>
                <p className="mt-1 truncate text-sm text-muted-foreground">
                  {user.email}
                </p>
              </CardContent>
            </Card>
          </aside>
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
      <CardHeader>
        <h2 className="section-title">Pacient</h2>
      </CardHeader>
      <CardContent>
        <p className="text-base font-semibold text-foreground">
          {patient.lastName} {patient.firstName}
        </p>
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

function NextStep({ consultation }: { consultation: Consultation }) {
  if (consultation.status === "CREATED") {
    return (
      <Alert variant="info" title="Următorul pas">
        Confirmă că pacientul a fost informat înainte de înregistrarea sau
        încărcarea audio.
      </Alert>
    );
  }

  if (consultation.status === "PATIENT_INFORMED") {
    return (
      <Alert variant="success" title="Următorul pas">
        Pacientul este informat. Poți continua cu înregistrarea sau încărcarea
        audio.
      </Alert>
    );
  }

  if (consultation.status === "TRANSCRIPTION_FAILED") {
    return (
      <Alert variant="error" title="Transcriere eșuată">
        Verifică fișierul audio și reia procesarea conform fluxului permis.
      </Alert>
    );
  }

  return (
    <Alert variant="info" title="Status procesare">
      Consultația este în etapa „{statusLabels[consultation.status]}”.
    </Alert>
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

import { cookies } from "next/headers";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import {
  GetConsultationApiError,
  getConsultation,
} from "@/features/consultations/api/get-consultation";
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
  PATIENT_INFORMED: "success",
  AUDIO_UPLOADED: "info",
  TRANSCRIBING: "processing",
  TRANSCRIPTION_READY: "success",
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
    <main className="min-h-screen bg-background">
      <div className="page-container py-7 sm:py-8">
        <header className="mb-7 border-b border-border pb-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <Badge variant={statusVariants[consultation.status]}>
                {statusLabels[consultation.status]}
              </Badge>
              <h1 className="page-title mt-3">Consultație</h1>
              <p className="secondary-text mt-2 max-w-2xl">
                Verifică datele consultației și continuă cu pașii clinici
                necesari pentru documentare.
              </p>
            </div>
            <Link
              href="/dashboard"
              className="inline-flex h-[var(--control-height)] items-center justify-center rounded-[var(--radius-control)] border border-border bg-surface px-4 text-sm font-medium text-foreground shadow-surface transition-colors hover:bg-surface-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
            >
              Înapoi la panou
            </Link>
          </div>
        </header>

        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
          <section className="space-y-6">
            <Card>
              <CardHeader>
                <h2 className="section-title">Detalii consultație</h2>
                <p className="secondary-text">
                  Identificatori și stare curentă pentru fluxul de lucru.
                </p>
              </CardHeader>
              <CardContent>
                <dl className="grid gap-4 sm:grid-cols-2">
                  <DetailItem label="ID consultație" value={consultation.id} />
                  <DetailItem label="ID pacient" value={consultation.patientId} />
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
          </section>

          <aside className="space-y-6">
            <PatientSummary patient={patient} patientId={consultation.patientId} />
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
    </main>
  );
}

function PatientSummary({
  patient,
  patientId,
}: {
  patient: Patient | null;
  patientId: string;
}) {
  if (!patient) {
    return (
      <Alert variant="warning" title="Date pacient indisponibile">
        Pacientul asociat nu a putut fi încărcat. ID pacient: {patientId}
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

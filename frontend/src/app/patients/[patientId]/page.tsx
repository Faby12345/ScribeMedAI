import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { ButtonLink } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { PageHeader } from "@/components/ui/page-header";
import { getCurrentUser } from "@/features/auth/api/current-user";
import {
  GetPatientApiError,
  getPatientById,
} from "@/features/patients/api/get-patient-server";
import { PatientAreaShell } from "@/features/patients/components/patient-area-shell";
import { PatientAvatar } from "@/features/patients/components/patient-avatar";
import {
  ageFromBirthDate,
  formatDate,
  formatDateTime,
  patientDisplayName,
  sexLabels,
  statusLabels,
} from "@/features/patients/components/patient-formatters";
import { StartPatientConsultationButton } from "@/features/patients/components/start-patient-consultation-button";
import type { Patient } from "@/features/patients/types";

type PatientProfilePageProps = {
  params: Promise<{
    patientId: string;
  }>;
};

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export default async function PatientProfilePage({
  params,
}: PatientProfilePageProps) {
  const { patientId } = await params;

  if (!uuidPattern.test(patientId)) {
    notFound();
  }

  const requestCookies = await cookies();
  const user = await getCurrentUser(requestCookies);

  if (!user) {
    redirect("/login");
  }

  let patient: Patient;

  try {
    patient = await getPatientById(patientId, requestCookies);
  } catch (error) {
    if (
      error instanceof GetPatientApiError &&
      (error.status === 401 || error.status === 403)
    ) {
      redirect("/login");
    }

    if (error instanceof GetPatientApiError && error.status === 404) {
      notFound();
    }

    throw error;
  }

  const age = ageFromBirthDate(patient.birthDate);

  return (
    <PatientAreaShell user={user} activeItem="patients">
      <PageHeader
        title={patientDisplayName(patient)}
        description="Profilul pacientului și contextul necesar pentru fluxul de consultație."
        actions={
          <>
            <Badge variant={patient.status === "ACTIVE" ? "info" : "neutral"}>
              {statusLabels[patient.status]}
            </Badge>
            <ButtonLink href="/patients" variant="outline">
              Înapoi la pacienți
            </ButtonLink>
            <ButtonLink href={`/patients/${patient.id}/edit`} variant="outline">
              Editează
            </ButtonLink>
            <StartPatientConsultationButton patientId={patient.id} />
          </>
        }
      />

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <section className="space-y-6">
          <Card>
            <CardHeader>
              <div className="flex items-start gap-4">
                <PatientAvatar patient={patient} className="size-12 text-base" />
                <div>
                  <h2 className="section-title">Prezentare pacient</h2>
                  <p className="secondary-text mt-1">
                    Date de identificare și contact pentru pacient.
                  </p>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <dl className="grid gap-4 sm:grid-cols-2">
                <DetailItem
                  label="Data nașterii"
                  value={formatDate(patient.birthDate)}
                />
                <DetailItem
                  label="Vârstă"
                  value={age === null ? "Nespecificată" : `${age} ani`}
                />
                <DetailItem
                  label="Sex"
                  value={patient.sex ? sexLabels[patient.sex] : "Nespecificat"}
                />
                <DetailItem
                  label="Telefon"
                  value={patient.phone ?? "Nespecificat"}
                />
                <DetailItem
                  label="Email"
                  value={patient.email ?? "Nespecificat"}
                />
              </dl>
            </CardContent>
          </Card>

          <section aria-labelledby="patient-consultations-title">
            <div className="mb-4">
              <h2 id="patient-consultations-title" className="section-title">
                Istoric consultații
              </h2>
              <p className="secondary-text mt-1">
                Consultațiile acestui pacient vor apărea aici când istoricul
                dedicat este disponibil.
              </p>
            </div>
            <EmptyState
              title="Nu există consultații afișate pentru acest pacient."
              description="Poți începe o consultație nouă din acțiunea principală a profilului."
            />
          </section>
        </section>

        <aside className="space-y-6">
          <Card variant="muted">
            <CardHeader>
              <h2 className="section-title">Status profil</h2>
            </CardHeader>
            <CardContent>
              <dl className="space-y-3">
                <DetailItem
                  label="Creat la"
                  value={formatDateTime(patient.createdAt)}
                />
                <DetailItem
                  label="Actualizat la"
                  value={formatDateTime(patient.updatedAt)}
                />
                <div>
                  <dt className="caption-text">Status</dt>
                  <dd className="mt-1">
                    <Badge
                      variant={
                        patient.status === "ACTIVE" ? "info" : "neutral"
                      }
                    >
                      {statusLabels[patient.status]}
                    </Badge>
                  </dd>
                </div>
              </dl>
            </CardContent>
          </Card>

          <Alert variant="info" title="Date minime">
            Profilul păstrează doar datele necesare MVP-ului. Nu introduce date
            medicale în câmpurile de contact.
          </Alert>
        </aside>
      </div>
    </PatientAreaShell>
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

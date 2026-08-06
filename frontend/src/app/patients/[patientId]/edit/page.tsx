import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";

import { Alert } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { ButtonLink } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input, Select } from "@/components/ui/input";
import { PageHeader } from "@/components/ui/page-header";
import { getCurrentUser } from "@/features/auth/api/current-user";
import {
  GetPatientApiError,
  getPatientById,
} from "@/features/patients/api/get-patient-server";
import { PatientAreaShell } from "@/features/patients/components/patient-area-shell";
import {
  patientDisplayName,
  sexLabels,
} from "@/features/patients/components/patient-formatters";
import type { Patient } from "@/features/patients/types";

type EditPatientPageProps = {
  params: Promise<{
    patientId: string;
  }>;
};

const uuidPattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export default async function EditPatientPage({ params }: EditPatientPageProps) {
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

  return (
    <PatientAreaShell user={user} activeItem="patients">
      <PageHeader
        title={`Editează ${patientDisplayName(patient)}`}
        description="Datele pot fi verificate aici. Modificarea profilului nu este disponibilă încă."
        actions={
          <>
            <Badge variant="warning">Editare indisponibilă</Badge>
            <ButtonLink href={`/patients/${patient.id}`} variant="outline">
              Înapoi la profil
            </ButtonLink>
          </>
        }
      />

      <section className="max-w-3xl" aria-labelledby="edit-patient-title">
        <Alert variant="warning" title="Salvarea nu este disponibilă încă">
          Profilul este afișat doar pentru verificare. Pentru corectarea
          datelor, folosește fluxul administrativ al clinicii.
        </Alert>

        <Card className="mt-5">
          <CardHeader>
            <h2 id="edit-patient-title" className="section-title">
              Date pacient
            </h2>
            <p className="secondary-text">
              Câmpurile sunt dezactivate până la implementarea fluxului de
              actualizare.
            </p>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="space-y-2">
                <span className="text-sm font-medium text-foreground">
                  Prenume
                </span>
                <Input value={patient.firstName} disabled readOnly />
              </label>
              <label className="space-y-2">
                <span className="text-sm font-medium text-foreground">
                  Nume
                </span>
                <Input value={patient.lastName} disabled readOnly />
              </label>
              <label className="space-y-2">
                <span className="text-sm font-medium text-foreground">
                  Data nașterii
                </span>
                <Input
                  type="date"
                  value={patient.birthDate ?? ""}
                  disabled
                  readOnly
                />
              </label>
              <label className="space-y-2">
                <span className="text-sm font-medium text-foreground">Sex</span>
                <Select value={patient.sex ?? ""} disabled>
                  <option value="">Nespecificat</option>
                  {Object.entries(sexLabels).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </Select>
              </label>
              <label className="space-y-2">
                <span className="text-sm font-medium text-foreground">
                  Telefon
                </span>
                <Input value={patient.phone ?? ""} disabled readOnly />
              </label>
              <label className="space-y-2">
                <span className="text-sm font-medium text-foreground">
                  Email
                </span>
                <Input value={patient.email ?? ""} disabled readOnly />
              </label>
            </div>
          </CardContent>
        </Card>
      </section>
    </PatientAreaShell>
  );
}

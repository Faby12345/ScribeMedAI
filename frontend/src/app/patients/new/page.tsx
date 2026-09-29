import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { ButtonLink } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { getCurrentUser } from "@/features/auth/api/current-user";
import { CreatePatientPageForm } from "@/features/patients/components/create-patient-page-form";
import { PatientAreaShell } from "@/features/patients/components/patient-area-shell";

export default async function NewPatientPage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return (
    <PatientAreaShell user={user} activeItem="patients">
      <div className="mx-auto w-full max-w-4xl">
        <PageHeader
          title="Adaugă pacient"
          description="Completează datele minime necesare pentru identificarea pacientului în fluxul de consultație."
          actions={
            <ButtonLink href="/patients" variant="outline">
              Înapoi la pacienți
            </ButtonLink>
          }
        />

        <section>
          <CreatePatientPageForm titleId="new-patient-form-title" />
        </section>
      </div>
    </PatientAreaShell>
  );
}

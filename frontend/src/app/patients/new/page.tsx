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
      <PageHeader
        title="Adaugă pacient"
        description="Completează datele minime necesare pentru identificarea pacientului în fluxul de consultație."
        actions={
          <ButtonLink href="/patients" variant="outline">
            Înapoi la pacienți
          </ButtonLink>
        }
      />

      <section className="max-w-3xl" aria-labelledby="new-patient-form-title">
        <div className="mb-4">
          <h2 id="new-patient-form-title" className="section-title">
            Date pacient
          </h2>
          <p className="secondary-text mt-1">
            Câmpurile nemarcate sunt opționale și pot fi completate ulterior
            când fluxul de editare va fi disponibil.
          </p>
        </div>
        <CreatePatientPageForm />
      </section>
    </PatientAreaShell>
  );
}

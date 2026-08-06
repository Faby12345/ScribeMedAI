import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { ButtonLink } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { getCurrentUser } from "@/features/auth/api/current-user";
import { PatientAreaShell } from "@/features/patients/components/patient-area-shell";
import { PatientList } from "@/features/patients/components/patient-list";

export default async function PatientsPage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return (
    <PatientAreaShell user={user} activeItem="patients">
      <PageHeader
        title="Pacienți"
        description="Caută rapid pacienții existenți, verifică datele de contact și deschide profilul clinic relevant."
        actions={
          <ButtonLink href="/patients/new">
            Adaugă pacient
          </ButtonLink>
        }
      />

      <section aria-labelledby="patients-list-title">
        <div className="mb-4">
          <h2 id="patients-list-title" className="section-title">
            Lista pacienților
          </h2>
          <p className="secondary-text mt-1">
            Căutarea este aplicată pe pacienții încărcați în această sesiune.
          </p>
        </div>
        <PatientList />
      </section>
    </PatientAreaShell>
  );
}

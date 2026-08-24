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
        title="Lista pacienților"
        description="Căutarea este aplicată pe pacienții încărcați în această sesiune."
        actions={
          <ButtonLink href="/dashboard" variant="outline">
            Înapoi la panou
          </ButtonLink>
        }
      />

      <section aria-label="Lista pacienților">
        <PatientList />
      </section>
    </PatientAreaShell>
  );
}

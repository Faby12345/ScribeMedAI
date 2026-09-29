import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { ButtonLink } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { getCurrentUser } from "@/features/auth/api/current-user";
import { MedicationList } from "@/features/medication/components/medication-list";
import { PatientAreaShell } from "@/features/patients/components/patient-area-shell";

export default async function MedicationsPage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return (
    <PatientAreaShell user={user} activeItem="medications" width="wide">
      <PageHeader
        title="Medicamente"
        description="Consultă nomenclatorul de medicamente și datele de autorizare disponibile."
        actions={
          <ButtonLink href="/dashboard" variant="outline">
            Înapoi la panou
          </ButtonLink>
        }
      />

      <section aria-labelledby="medications-list-title">
        <h2 id="medications-list-title" className="sr-only">
          Nomenclator de medicamente
        </h2>
        <MedicationList />
      </section>
    </PatientAreaShell>
  );
}

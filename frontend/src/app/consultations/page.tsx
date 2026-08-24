import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { ButtonLink } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { getCurrentUser } from "@/features/auth/api/current-user";
import { ConsultationList } from "@/features/consultations/components/consultation-list";
import { PatientAreaShell } from "@/features/patients/components/patient-area-shell";

export default async function ConsultationsPage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return (
    <PatientAreaShell user={user} activeItem="consultations">
      <PageHeader
        title="Consultații"
        description="Vezi consultațiile create în clinică și continuă fluxul de documentare pentru fiecare pacient."
        actions={
          <ButtonLink href="/dashboard" variant="outline">
            Înapoi la panou
          </ButtonLink>
        }
      />

      <section>
        <div className="mb-4">
        </div>
        <ConsultationList />
      </section>
    </PatientAreaShell>
  );
}

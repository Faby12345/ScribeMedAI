import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { ButtonLink } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { getCurrentUser } from "@/features/auth/api/current-user";
import { ConsultationList } from "@/features/consultations/components/consultation-list";

export default async function ConsultationsPage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return (
    <div className="page-container py-7 sm:py-8">
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
          <h2 className="section-title">Lista consultațiilor</h2>
          <p className="secondary-text mt-1">
            Consultațiile sunt afișate de la cele mai recente la cele mai vechi.
          </p>
        </div>
        <ConsultationList />
      </section>
    </div>
  );
}

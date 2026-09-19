import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { ButtonLink } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { getCurrentUser } from "@/features/auth/api/current-user";
import { KnowledgeDocumentUpload } from "@/features/knowledge/components/knowledge-document-upload";
import { PatientAreaShell } from "@/features/patients/components/patient-area-shell";

export default async function KnowledgePage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return (
    <PatientAreaShell user={user} activeItem="knowledge">
      <PageHeader
        title="Bibliotecă de cunoștințe"
        description="Adaugă surse medicale PDF care vor putea fi consultate de sistem atunci când generează răspunsuri bazate pe documente."
        actions={
          <ButtonLink href="/dashboard" variant="outline">
            Înapoi la panou
          </ButtonLink>
        }
      />

      <KnowledgeDocumentUpload />
    </PatientAreaShell>
  );
}

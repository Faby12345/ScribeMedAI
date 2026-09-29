import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { getCurrentUser } from "@/features/auth/api/current-user";
import { KnowledgeLibrary } from "@/features/knowledge/components/knowledge-library";
import { PatientAreaShell } from "@/features/patients/components/patient-area-shell";

export default async function KnowledgePage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return (
    <PatientAreaShell user={user} activeItem="knowledge">
      <KnowledgeLibrary />
    </PatientAreaShell>
  );
}

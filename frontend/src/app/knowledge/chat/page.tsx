import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { getCurrentUser } from "@/features/auth/api/current-user";
import { KnowledgeChatWorkspace } from "@/features/knowledge/components/knowledge-chat-workspace";
import { PatientAreaShell } from "@/features/patients/components/patient-area-shell";

export default async function KnowledgeChatPage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return (
    <>
      <template
        data-impeccable-contract="knowledge-chat-0d6bf046"
        dangerouslySetInnerHTML={{
          __html: `<!--
THESIS: Conversația este masa de lucru; sursele rămân vizibile și verificabile, nu ascunse într-un dialog.
OWN-WORLD: Suprafețe albe și slate, albastru medical semantic, linii fine, raze și tipografie moștenite din ScribeMedAI.
STORY: Medicul alege sursele, întreabă, urmărește consultarea fragmentelor și verifică răspunsul lângă citări.
FIRST VIEWPORT: Rail de documente la stânga, conversație dominantă la dreapta, compozitor ancorat la baza spațiului de lucru.
FORM: Clinical reading desk, candidatul structural 7, seed 0d6bf046.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, and DESIGN.md
-->`,
        }}
      />
      <PatientAreaShell
        user={user}
        activeItem="knowledge"
        width="wide"
        layout="workspace"
      >
        <div className="min-h-0 flex-1">
          <KnowledgeChatWorkspace />
        </div>
      </PatientAreaShell>
    </>
  );
}

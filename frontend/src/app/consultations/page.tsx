import { cookies } from "next/headers";
import Link from "next/link";
import { redirect } from "next/navigation";

import { Badge } from "@/components/ui/badge";
import { getCurrentUser } from "@/features/auth/api/current-user";
import { ConsultationList } from "@/features/consultations/components/consultation-list";

export default async function ConsultationsPage() {
  const user = await getCurrentUser(await cookies());

  if (!user) {
    redirect("/login");
  }

  return (
    <main className="min-h-screen bg-background">
      <div className="page-container py-7 sm:py-8">
        <header className="mb-7 border-b border-border pb-6">
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <Badge variant="info">Istoric clinic</Badge>
              <h1 className="page-title mt-3">Consultații</h1>
              <p className="secondary-text mt-2 max-w-2xl">
                Vezi consultațiile create în clinică și continuă fluxul de
                documentare pentru fiecare pacient.
              </p>
            </div>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Link
                href="/dashboard"
                className="inline-flex h-[var(--control-height)] items-center justify-center rounded-[var(--radius-control)] border border-border bg-surface px-4 text-sm font-medium text-foreground shadow-surface transition-colors hover:bg-surface-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
              >
                Înapoi la panou
              </Link>
            </div>
          </div>
        </header>

        <section>
          <div className="mb-4">
            <h2 className="section-title">Lista consultațiilor</h2>
            <p className="secondary-text mt-1">
              Consultațiile sunt afișate de la cele mai recente la cele mai
              vechi.
            </p>
          </div>
          <ConsultationList />
        </section>
      </div>
    </main>
  );
}

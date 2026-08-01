"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { LogoutButton } from "@/features/auth/components/logout-button";
import type { AuthenticatedUser } from "@/features/auth/types";
import { NewConsultationPanel } from "@/features/consultations/components/new-consultation-panel";
import { PatientList } from "@/features/patients/components/patient-list";
import { cn } from "@/lib/class-names";

type DashboardShellProps = {
  user: AuthenticatedUser;
};

const navigationItems = [
  { label: "Panou principal", active: true },
  { label: "Pacienți", active: false },
  { label: "Consultații", active: false },
  { label: "Documente", active: false },
];

export function DashboardShell({ user }: DashboardShellProps) {
  const [isNewConsultationOpen, setIsNewConsultationOpen] = useState(false);
  const [patientsRefreshKey, setPatientsRefreshKey] = useState(0);

  return (
    <main className="min-h-screen bg-background">
      <div className="flex min-h-screen">
        <aside className="hidden w-[var(--sidebar-width)] border-r border-border bg-surface lg:flex lg:flex-col">
          <div className="flex h-[var(--header-height)] items-center gap-3 border-b border-border px-5">
            <div
              className="flex size-9 items-center justify-center rounded-[var(--radius-control)] bg-primary text-sm font-semibold text-primary-foreground"
              aria-hidden="true"
            >
              SM
            </div>
            <div>
              <p className="text-sm font-semibold text-foreground">
                ScribeMedAI
              </p>
              <p className="caption-text">Spațiu clinic</p>
            </div>
          </div>

          <nav className="flex-1 px-3 py-4" aria-label="Navigație principală">
            <ul className="space-y-1">
              {navigationItems.map((item) => (
                <li key={item.label}>
                  <a
                    href="#"
                    className={cn(
                      "flex h-10 items-center rounded-[var(--radius-control)] px-3 text-sm font-medium transition-colors",
                      item.active
                        ? "bg-primary-soft text-primary"
                        : "text-muted-foreground hover:bg-secondary hover:text-foreground",
                    )}
                    aria-current={item.active ? "page" : undefined}
                  >
                    {item.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>

          <div className="border-t border-border p-4">
            <div className="mb-3">
              <p className="text-sm font-medium text-foreground">
                {user.displayName}
              </p>
              <p className="truncate text-sm text-muted-foreground">
                {user.email}
              </p>
            </div>
            <LogoutButton />
          </div>
        </aside>

        <section className="min-w-0 flex-1">
          <header className="flex h-[var(--header-height)] items-center border-b border-border bg-surface lg:hidden">
            <div className="flex w-full items-center justify-between gap-4 px-5">
              <div className="flex items-center gap-3">
                <div
                  className="flex size-9 items-center justify-center rounded-[var(--radius-control)] bg-primary text-sm font-semibold text-primary-foreground"
                  aria-hidden="true"
                >
                  SM
                </div>
                <div>
                  <p className="text-sm font-semibold text-foreground">
                    ScribeMedAI
                  </p>
                  <p className="caption-text">Panou clinic</p>
                </div>
              </div>
              <LogoutButton compact />
            </div>
          </header>

          <div className="page-container py-7 sm:py-8">
            <section className="mb-7 flex flex-col gap-4 border-b border-border pb-6 md:flex-row md:items-end md:justify-between">
              <div>
                <Badge variant="info">Sesiune activă</Badge>
                <h1 className="page-title mt-3">Panou clinic</h1>
                <p className="secondary-text mt-2 max-w-2xl">
                  Bun venit, {user.displayName}. De aici începe fluxul pentru
                  pacienți, consultații și documente medicale.
                </p>
              </div>
              <div className="flex flex-col gap-2 sm:flex-row">
                <Button variant="outline" disabled>
                  Caută pacient
                </Button>
                <Button
                  type="button"
                  variant="primary"
                  onClick={() => setIsNewConsultationOpen(true)}
                >
                  Consultație nouă
                </Button>
              </div>
            </section>

            <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
              <section>
                <div className="mb-4 flex items-center justify-between gap-4">
                  <div>
                    <h2 className="section-title">Pacienți</h2>
                    <p className="secondary-text mt-1">
                      Caută un pacient existent sau adaugă unul nou înainte de
                      crearea consultației.
                    </p>
                  </div>
                </div>

                <PatientList
                  refreshKey={patientsRefreshKey}
                  onCreatePatient={() => setIsNewConsultationOpen(true)}
                />
              </section>

              <aside className="space-y-6">
                <section>
                  <h2 className="section-title">Consultații recente</h2>
                  <p className="secondary-text mt-1">
                    Aici vor apărea consultațiile începute sau finalizate.
                  </p>

                  <Card variant="standard" className="mt-4 p-5">
                    <div className="rounded-[var(--radius-control)] border border-dashed border-border bg-surface-muted p-5">
                      <p className="text-sm font-medium text-foreground">
                        Nu există consultații afișate încă.
                      </p>
                      <p className="secondary-text mt-2">
                        După implementarea consultațiilor, lista va fi
                        actualizată automat.
                      </p>
                    </div>
                  </Card>
                </section>

                <section>
                  <h2 className="section-title">Status flux</h2>
                  <div className="mt-4 space-y-2">
                    <StatusRow label="Autentificare" status="Activ" />
                    <StatusRow label="Pacienți" status="Urmează" muted />
                    <StatusRow label="Consultații" status="Urmează" muted />
                  </div>
                </section>
              </aside>
            </div>
          </div>
        </section>
      </div>
      <NewConsultationPanel
        isOpen={isNewConsultationOpen}
        onClose={() => setIsNewConsultationOpen(false)}
        onPatientCreated={() =>
          setPatientsRefreshKey((current) => current + 1)
        }
      />
    </main>
  );
}

function StatusRow({
  label,
  status,
  muted = false,
}: {
  label: string;
  status: string;
  muted?: boolean;
}) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-[var(--radius-control)] border border-border bg-surface px-3 py-2">
      <span
        className={cn(
          "text-sm",
          muted ? "text-muted-foreground" : "text-foreground",
        )}
      >
        {label}
      </span>
      <Badge variant={muted ? "neutral" : "success"}>{status}</Badge>
    </div>
  );
}

"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";

import { LogoutButton } from "@/features/auth/components/logout-button";
import { cn } from "@/lib/class-names";

type AppShellProps = {
  children: ReactNode;
};

const publicRoutes = new Set(["/", "/login"]);

const navigationItems = [
  { label: "Panou", href: "/dashboard", matcher: "/dashboard" },
  { label: "Pacienți", href: "/patients", matcher: "/patients" },
  { label: "Consultații", href: "/consultations", matcher: "/consultations" },
  { label: "Medicamente", href: "/medications", matcher: "/medications" },
  { label: "Cunoștințe", href: "/knowledge", matcher: "/knowledge" },
];

export function AppShell({ children }: AppShellProps) {
  const pathname = usePathname();

  if (publicRoutes.has(pathname)) {
    return <>{children}</>;
  }

  return (
    <div className="min-h-screen overflow-x-clip bg-[radial-gradient(circle_at_16%_10%,#eaf8ff_0%,transparent_30%),linear-gradient(180deg,#fbfdff_0%,#ffffff_46%,#f6f9fd_100%)] text-foreground">
      <header className="sticky top-0 z-30 border-b border-border/75 bg-white/92">
        <div className="mx-auto grid min-h-[4.25rem] w-full max-w-7xl grid-cols-[minmax(0,1fr)_auto] gap-3 px-4 py-3 sm:px-6 lg:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)] lg:items-center lg:px-8">
          <Link
            href="/dashboard"
            className="flex min-w-0 items-center gap-3 rounded-[var(--radius-control)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <span
              className="flex size-9 items-center justify-center rounded-[var(--radius-control)] bg-[linear-gradient(145deg,#1d5f9f_0%,#5bb8d7_100%)] text-sm font-semibold text-primary-foreground shadow-surface"
              aria-hidden="true"
            >
              SM
            </span>
            <span className="min-w-0">
              <span className="block truncate text-sm font-semibold text-foreground">
                ScribeMedAI
              </span>
              <span className="caption-text block truncate">
                Documentare asistată de AI
              </span>
            </span>
          </Link>

          <nav
            className="order-3 col-span-2 flex max-w-full gap-1 overflow-x-auto rounded-[calc(var(--radius-control)+0.25rem)] border border-border bg-surface-muted p-1 lg:order-none lg:col-span-1"
            aria-label="Navigație principală"
          >
            {navigationItems.map((item) => {
              const isActive = pathname.startsWith(item.matcher);

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={cn(
                    "whitespace-nowrap rounded-[var(--radius-control)] px-3 py-2 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
                    isActive
                      ? "bg-[linear-gradient(135deg,#ffffff_0%,#eaf6ff_100%)] text-primary shadow-surface"
                      : "text-muted-foreground hover:bg-white hover:text-foreground",
                  )}
                  aria-current={isActive ? "page" : undefined}
                >
                  {item.label}
                </Link>
              );
            })}
          </nav>

          <div className="flex min-w-0 justify-end">
            <LogoutButton compact />
          </div>
        </div>
      </header>

      <main>{children}</main>
    </div>
  );
}

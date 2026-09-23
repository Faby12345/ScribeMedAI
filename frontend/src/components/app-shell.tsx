"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
  useEffect,
  useRef,
  useState,
} from "react";

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
  const navigationDialogRef = useRef<HTMLDialogElement>(null);
  const closeTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [isNavigationClosing, setIsNavigationClosing] = useState(false);

  useEffect(() => {
    return () => {
      if (closeTimeoutRef.current) {
        clearTimeout(closeTimeoutRef.current);
      }
    };
  }, []);

  function openNavigation() {
    const dialog = navigationDialogRef.current;

    if (!dialog || dialog.open) {
      return;
    }

    setIsNavigationClosing(false);
    dialog.showModal();
  }

  function closeNavigation() {
    const dialog = navigationDialogRef.current;

    if (!dialog?.open || isNavigationClosing) {
      return;
    }

    setIsNavigationClosing(true);
    closeTimeoutRef.current = setTimeout(() => {
      dialog.close();
      setIsNavigationClosing(false);
      closeTimeoutRef.current = null;
    }, 180);
  }

  function handleDialogClick(event: ReactMouseEvent<HTMLDialogElement>) {
    if (event.target === event.currentTarget) {
      closeNavigation();
    }
  }

  if (publicRoutes.has(pathname)) {
    return <>{children}</>;
  }

  return (
    <div className="min-h-screen overflow-x-clip bg-[radial-gradient(circle_at_16%_10%,#eaf8ff_0%,transparent_30%),linear-gradient(180deg,#fbfdff_0%,#ffffff_46%,#f6f9fd_100%)] text-foreground">
      <header className="sticky top-0 z-30 border-b border-border/75 bg-white/92">
        <div className="mx-auto flex min-h-[4.25rem] w-full max-w-7xl items-center gap-3 px-4 py-3 sm:px-6 lg:px-8">
          <button
            type="button"
            onClick={openNavigation}
            className="group flex size-11 shrink-0 flex-col items-center justify-center gap-[0.3rem] rounded-[var(--radius-control)] border border-border bg-surface text-foreground shadow-surface transition-colors hover:bg-surface-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
            aria-label="Deschide meniul principal"
            aria-haspopup="dialog"
            aria-controls="main-navigation-drawer"
          >
            <span className="h-0.5 w-5 rounded-full bg-current transition-transform duration-150 group-hover:-translate-x-0.5" />
            <span className="h-0.5 w-5 rounded-full bg-current" />
            <span className="h-0.5 w-5 rounded-full bg-current transition-transform duration-150 group-hover:translate-x-0.5" />
          </button>

          <Link
            href="/dashboard"
            className="flex min-w-0 items-center gap-3 rounded-[var(--radius-control)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background"
          >
            <span className="flex size-9 items-center justify-center rounded-[var(--radius-control)] bg-primary text-sm font-semibold text-primary-foreground shadow-surface" aria-hidden="true">
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
        </div>
      </header>

      <dialog
        ref={navigationDialogRef}
        id="main-navigation-drawer"
        className="navigation-drawer fixed inset-0 m-0 h-dvh max-h-none w-full max-w-none bg-transparent p-0"
        data-closing={isNavigationClosing ? "true" : "false"}
        onCancel={(event) => {
          event.preventDefault();
          closeNavigation();
        }}
        onClick={handleDialogClick}
        onClose={() => setIsNavigationClosing(false)}
        aria-labelledby="main-navigation-title"
      >
        <aside className="navigation-drawer-panel flex h-full w-[min(20rem,calc(100vw-1.5rem))] flex-col border-r border-border bg-surface shadow-elevated">
          <div className="flex min-h-[4.25rem] items-center justify-between gap-4 border-b border-border px-5 py-3">
            <div className="min-w-0">
              <p id="main-navigation-title" className="font-semibold text-foreground">
                Meniu principal
              </p>
              <p className="caption-text mt-0.5 truncate">ScribeMedAI</p>
            </div>
            <button
              type="button"
              onClick={closeNavigation}
              className="flex size-10 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              aria-label="Închide meniul"
            >
              <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                aria-hidden="true"
                className="size-5"
              >
                <path d="M6 6l12 12M18 6L6 18" />
              </svg>
            </button>
          </div>

          <nav className="flex-1 overflow-y-auto px-3 py-5" aria-label="Navigație principală">
            <ul className="space-y-1">
              {navigationItems.map((item) => {
                const isActive = pathname.startsWith(item.matcher);

                return (
                  <li key={item.href}>
                    <Link
                      href={item.href}
                      onClick={closeNavigation}
                      className={cn(
                        "flex min-h-11 items-center justify-between gap-3 rounded-[var(--radius-control)] px-3.5 py-2.5 text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset",
                        isActive
                          ? "bg-primary-soft font-semibold text-primary"
                          : "font-medium text-muted-foreground hover:bg-surface-muted hover:text-foreground",
                      )}
                      aria-current={isActive ? "page" : undefined}
                    >
                      <span>{item.label}</span>
                      {isActive ? (
                        <span className="size-1.5 rounded-full bg-primary" aria-hidden="true" />
                      ) : null}
                    </Link>
                  </li>
                );
              })}
            </ul>
          </nav>

          <div className="border-t border-border bg-surface-muted px-5 py-5">
            <p className="caption-text mb-3">Sesiunea medicului</p>
            <div className="w-full">
              <LogoutButton />
            </div>
          </div>
        </aside>
      </dialog>

      <main>{children}</main>
    </div>
  );
}

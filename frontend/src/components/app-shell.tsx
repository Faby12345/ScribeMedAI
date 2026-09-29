"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  type ComponentType,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
  type SVGProps,
  useEffect,
  useRef,
  useState,
  useSyncExternalStore,
} from "react";

import { LogoutButton } from "@/features/auth/components/logout-button";
import { cn } from "@/lib/class-names";

type AppShellProps = {
  children: ReactNode;
};

type NavigationIcon = ComponentType<SVGProps<SVGSVGElement>>;

type NavigationItem = {
  label: string;
  href: string;
  matcher: string;
  icon: NavigationIcon;
  exact?: boolean;
};

const publicRoutes = new Set(["/", "/login"]);
const sidebarPreferenceKey = "scribemed.sidebar.expanded";
const sidebarPreferenceEvent = "scribemed:sidebar-preference-change";
let fallbackSidebarPreference = true;

const navigationItems: NavigationItem[] = [
  {
    label: "Panou",
    href: "/dashboard",
    matcher: "/dashboard",
    icon: DashboardIcon,
  },
  {
    label: "Pacienți",
    href: "/patients",
    matcher: "/patients",
    icon: PatientsIcon,
  },
  {
    label: "Consultații",
    href: "/consultations",
    matcher: "/consultations",
    icon: ConsultationsIcon,
  },
  {
    label: "Medicamente",
    href: "/medications",
    matcher: "/medications",
    icon: MedicationsIcon,
  },
  {
    label: "Bibliotecă",
    href: "/knowledge",
    matcher: "/knowledge",
    icon: LibraryIcon,
    exact: true,
  },
  {
    label: "Asistent documente",
    href: "/knowledge/chat",
    matcher: "/knowledge/chat",
    icon: AssistantIcon,
  },
];

export function AppShell({ children }: AppShellProps) {
  const pathname = usePathname();
  const navigationDialogRef = useRef<HTMLDialogElement>(null);
  const closeTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const isSidebarExpanded = useSyncExternalStore(
    subscribeToSidebarPreference,
    readSidebarPreference,
    () => true,
  );
  const [isNavigationClosing, setIsNavigationClosing] = useState(false);
  const [isMobileNavigationOpen, setIsMobileNavigationOpen] = useState(false);

  useEffect(() => {
    return () => {
      if (closeTimeoutRef.current) {
        clearTimeout(closeTimeoutRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const desktopMedia = window.matchMedia("(min-width: 1024px)");

    function closeDrawerOnDesktop(event: MediaQueryListEvent) {
      const dialog = navigationDialogRef.current;
      if (!event.matches || !dialog?.open) {
        return;
      }

      if (closeTimeoutRef.current) {
        clearTimeout(closeTimeoutRef.current);
        closeTimeoutRef.current = null;
      }
      dialog.close();
      setIsNavigationClosing(false);
      setIsMobileNavigationOpen(false);
    }

    desktopMedia.addEventListener("change", closeDrawerOnDesktop);
    return () => desktopMedia.removeEventListener("change", closeDrawerOnDesktop);
  }, []);

  function toggleSidebar() {
    const nextState = !isSidebarExpanded;
    fallbackSidebarPreference = nextState;
    document.documentElement.dataset.sidebarExpanded = String(nextState);
    try {
      window.localStorage.setItem(sidebarPreferenceKey, String(nextState));
    } catch {
      // The in-memory fallback keeps the control usable when storage is blocked.
    }
    window.dispatchEvent(new Event(sidebarPreferenceEvent));
  }

  function openNavigation() {
    const dialog = navigationDialogRef.current;
    if (!dialog || dialog.open) {
      return;
    }

    setIsNavigationClosing(false);
    setIsMobileNavigationOpen(true);
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
      setIsMobileNavigationOpen(false);
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

  const currentPage =
    navigationItems.find((item) => isNavigationItemActive(item, pathname))
      ?.label ?? "ScribeMedAI";

  return (
    <div className="min-h-screen overflow-x-clip bg-[radial-gradient(circle_at_16%_10%,#eaf8ff_0%,transparent_30%),linear-gradient(180deg,#fbfdff_0%,#ffffff_46%,#f6f9fd_100%)] text-foreground">
      <aside
        id="desktop-navigation"
        data-desktop-sidebar
        className={cn(
          "fixed inset-y-0 left-0 z-40 hidden flex-col border-r border-border bg-surface transition-[width] duration-200 ease-out motion-reduce:transition-none lg:flex",
          isSidebarExpanded ? "w-64" : "w-[4.5rem]",
        )}
        aria-label="Navigație principală"
      >
        <div
          className={cn(
            "flex min-h-[4.25rem] items-center border-b border-border px-3",
            isSidebarExpanded ? "gap-2" : "justify-center",
          )}
        >
          <SidebarToggle
            expanded={isSidebarExpanded}
            onToggle={toggleSidebar}
          />
          {isSidebarExpanded ? <BrandLink /> : null}
        </div>

        <nav className="flex-1 px-2 py-4" aria-label="Pagini ScribeMedAI">
          <NavigationLinks
            pathname={pathname}
            collapsed={!isSidebarExpanded}
          />
        </nav>

        <div
          className={cn(
            "border-t border-border bg-surface-muted",
            isSidebarExpanded ? "p-4" : "flex justify-center p-3",
          )}
        >
          {isSidebarExpanded ? (
            <>
              <p className="caption-text mb-3">Sesiunea medicului</p>
              <LogoutButton />
            </>
          ) : (
            <LogoutButton compact />
          )}
        </div>
      </aside>

      <div
        data-app-shell-content
        className={cn(
          "min-h-screen transition-[padding] duration-200 ease-out motion-reduce:transition-none",
          isSidebarExpanded ? "lg:pl-64" : "lg:pl-[4.5rem]",
        )}
      >
        <header className="sticky top-0 z-30 border-b border-border/75 bg-white/95">
          <div className="flex min-h-[4.25rem] w-full items-center gap-3 px-4 py-3 sm:px-6 lg:px-7">
            <button
              type="button"
              onClick={openNavigation}
              className="flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-foreground transition-colors hover:bg-surface-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background lg:hidden"
              aria-label="Deschide meniul principal"
              aria-expanded={isMobileNavigationOpen}
              aria-haspopup="dialog"
              aria-controls="main-navigation-drawer"
            >
              <MenuIcon />
            </button>

            <div className="lg:hidden">
              <BrandLink />
            </div>

            <p className="hidden text-sm font-semibold text-foreground lg:block">
              {currentPage}
            </p>
          </div>
        </header>

        <main>{children}</main>
      </div>

      <dialog
        ref={navigationDialogRef}
        id="main-navigation-drawer"
        className="navigation-drawer fixed inset-0 m-0 h-dvh max-h-none w-full max-w-none bg-transparent p-0 lg:hidden"
        data-closing={isNavigationClosing ? "true" : "false"}
        onCancel={(event) => {
          event.preventDefault();
          closeNavigation();
        }}
        onClick={handleDialogClick}
        onClose={() => {
          setIsNavigationClosing(false);
          setIsMobileNavigationOpen(false);
        }}
        aria-labelledby="main-navigation-title"
      >
        <aside className="navigation-drawer-panel flex h-full w-[min(20rem,calc(100vw-1.5rem))] flex-col border-r border-border bg-surface shadow-elevated">
          <div className="flex min-h-[4.25rem] items-center justify-between gap-4 border-b border-border px-4 py-3">
            <div id="main-navigation-title">
              <BrandLink />
            </div>
            <button
              type="button"
              onClick={closeNavigation}
              className="flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              aria-label="Închide meniul principal"
            >
              <CloseIcon />
            </button>
          </div>

          <nav className="flex-1 overflow-y-auto px-3 py-4" aria-label="Pagini ScribeMedAI">
            <NavigationLinks pathname={pathname} onNavigate={closeNavigation} />
          </nav>

          <div className="border-t border-border bg-surface-muted px-4 py-4">
            <p className="caption-text mb-3">Sesiunea medicului</p>
            <LogoutButton />
          </div>
        </aside>
      </dialog>
    </div>
  );
}

function NavigationLinks({
  pathname,
  collapsed = false,
  onNavigate,
}: {
  pathname: string;
  collapsed?: boolean;
  onNavigate?: () => void;
}) {
  return (
    <ul className="space-y-1">
      {navigationItems.map((item) => {
        const Icon = item.icon;
        const isActive = isNavigationItemActive(item, pathname);

        return (
          <li key={item.href} className="relative">
            <Link
              href={item.href}
              onClick={onNavigate}
              aria-label={collapsed ? item.label : undefined}
              aria-current={isActive ? "page" : undefined}
              className={cn(
                "group relative flex min-h-11 items-center rounded-[var(--radius-control)] text-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-inset",
                collapsed ? "justify-center px-2" : "gap-3 px-3 py-2.5",
                isActive
                  ? "bg-primary-soft font-semibold text-primary"
                  : "font-medium text-muted-foreground hover:bg-surface-muted hover:text-foreground",
              )}
            >
              <Icon className="size-5 shrink-0" aria-hidden="true" />
              {!collapsed ? <span className="truncate">{item.label}</span> : null}
              {collapsed ? (
                <span
                  aria-hidden="true"
                  className="pointer-events-none absolute left-[calc(100%+0.625rem)] top-1/2 z-50 -translate-y-1/2 whitespace-nowrap rounded-[var(--radius-control)] bg-foreground px-2.5 py-1.5 text-xs font-medium text-white opacity-0 shadow-elevated transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100"
                >
                  {item.label}
                </span>
              ) : null}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

function SidebarToggle({
  expanded,
  onToggle,
}: {
  expanded: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className="flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-control)] text-foreground transition-colors hover:bg-surface-muted focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      aria-label={expanded ? "Restrânge bara laterală" : "Extinde bara laterală"}
      aria-expanded={expanded}
      aria-controls="desktop-navigation"
      title={expanded ? "Restrânge bara laterală" : "Extinde bara laterală"}
    >
      <SidebarIcon expanded={expanded} />
    </button>
  );
}

function BrandLink() {
  return (
    <Link
      href="/dashboard"
      className="flex min-w-0 items-center gap-2.5 rounded-[var(--radius-control)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
    >
      <span className="flex size-8 shrink-0 items-center justify-center rounded-[var(--radius-control)] bg-primary text-xs font-semibold text-primary-foreground" aria-hidden="true">
        SM
      </span>
      <span className="truncate text-sm font-semibold text-foreground">ScribeMedAI</span>
    </Link>
  );
}

function isNavigationItemActive(item: NavigationItem, pathname: string) {
  return item.exact ? pathname === item.matcher : pathname.startsWith(item.matcher);
}

function subscribeToSidebarPreference(onStoreChange: () => void) {
  function handlePreferenceChange() {
    document.documentElement.dataset.sidebarExpanded = String(
      readSidebarPreference(),
    );
    onStoreChange();
  }

  window.addEventListener("storage", handlePreferenceChange);
  window.addEventListener(sidebarPreferenceEvent, handlePreferenceChange);

  return () => {
    window.removeEventListener("storage", handlePreferenceChange);
    window.removeEventListener(sidebarPreferenceEvent, handlePreferenceChange);
  };
}

function readSidebarPreference() {
  try {
    const savedPreference = window.localStorage.getItem(sidebarPreferenceKey);
    return savedPreference === null
      ? fallbackSidebarPreference
      : savedPreference === "true";
  } catch {
    return fallbackSidebarPreference;
  }
}

function IconBase({ className, children, ...props }: SVGProps<SVGSVGElement>) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      {...props}
    >
      {children}
    </svg>
  );
}

function DashboardIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <IconBase {...props}>
      <path d="m4 10 8-6 8 6v9a1 1 0 0 1-1 1h-5v-6h-4v6H5a1 1 0 0 1-1-1v-9Z" />
    </IconBase>
  );
}

function PatientsIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <IconBase {...props}>
      <path d="M16 20v-1.5a4.5 4.5 0 0 0-4.5-4.5h-3A4.5 4.5 0 0 0 4 18.5V20" />
      <circle cx="10" cy="7" r="3" />
      <path d="M17 11a3 3 0 0 0 0-6M18 14a4 4 0 0 1 3 4v2" />
    </IconBase>
  );
}

function ConsultationsIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <IconBase {...props}>
      <rect x="4" y="5" width="16" height="15" rx="2" />
      <path d="M8 3v4M16 3v4M4 10h16M8 14h3M8 17h6" />
    </IconBase>
  );
}

function MedicationsIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <IconBase {...props}>
      <path d="m5.3 18.7 9.4-9.4a4 4 0 0 1 5.7 5.7L11 24.4a4 4 0 0 1-5.7-5.7Z" transform="translate(0 -3)" />
      <path d="m9 15 5 5" />
    </IconBase>
  );
}

function LibraryIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <IconBase {...props}>
      <path d="M5 4.5A2.5 2.5 0 0 1 7.5 2H11v17H7.5A2.5 2.5 0 0 0 5 21.5v-17Z" />
      <path d="M19 4.5A2.5 2.5 0 0 0 16.5 2H13v17h3.5a2.5 2.5 0 0 1 2.5 2.5v-17Z" />
    </IconBase>
  );
}

function AssistantIcon(props: SVGProps<SVGSVGElement>) {
  return (
    <IconBase {...props}>
      <path d="M5 5h10a3 3 0 0 1 3 3v5a3 3 0 0 1-3 3H9l-4 3v-3a3 3 0 0 1-2-3V8a3 3 0 0 1 2-3Z" />
      <path d="m18.5 3 .5 1.5L20.5 5 19 5.5 18.5 7 18 5.5 16.5 5l1.5-.5.5-1.5Z" />
    </IconBase>
  );
}

function SidebarIcon({ expanded }: { expanded: boolean }) {
  return (
    <IconBase className="size-5" aria-hidden="true">
      <rect x="3" y="4" width="18" height="16" rx="2" />
      <path d="M9 4v16" />
      <path d={expanded ? "m15 9-3 3 3 3" : "m13 9 3 3-3 3"} />
    </IconBase>
  );
}

function MenuIcon() {
  return (
    <IconBase className="size-5" aria-hidden="true">
      <path d="M4 7h16M4 12h16M4 17h16" />
    </IconBase>
  );
}

function CloseIcon() {
  return (
    <IconBase className="size-5" aria-hidden="true">
      <path d="M6 6l12 12M18 6 6 18" />
    </IconBase>
  );
}

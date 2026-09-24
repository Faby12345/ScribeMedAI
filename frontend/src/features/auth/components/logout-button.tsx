"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { LogoutApiError, logout } from "@/features/auth/api/logout";

type LogoutButtonProps = {
  compact?: boolean;
};

export function LogoutButton({ compact = false }: LogoutButtonProps) {
  const router = useRouter();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleLogout() {
    if (isLoggingOut) {
      return;
    }

    setIsLoggingOut(true);
    setError(null);

    try {
      await logout();
      router.replace("/login");
      router.refresh();
    } catch (logoutError) {
      if (logoutError instanceof LogoutApiError) {
        setError(logoutError.message);
      } else {
        setError("Deconectarea nu a reușit. Încearcă din nou.");
      }
      setIsLoggingOut(false);
    }
  }

  return (
    <div className="relative flex flex-col items-end gap-2">
      <Button
        type="button"
        variant="destructive"
        size={compact ? "icon" : "sm"}
        isLoading={isLoggingOut}
        loadingText={compact ? undefined : "Se deconectează"}
        onClick={handleLogout}
        className={compact ? "group relative size-11 p-0" : "w-full"}
        aria-label={compact ? "Deconectare" : undefined}
      >
        {compact ? <LogoutIcon /> : "Deconectare"}
        {compact ? (
          <span
            aria-hidden="true"
            className="pointer-events-none absolute left-[calc(100%+0.625rem)] top-1/2 z-50 -translate-y-1/2 whitespace-nowrap rounded-[var(--radius-control)] bg-foreground px-2.5 py-1.5 text-xs font-medium text-white opacity-0 shadow-elevated transition-opacity group-hover:opacity-100 group-focus-visible:opacity-100"
          >
            Deconectare
          </span>
        ) : null}
      </Button>
      {error ? (
        <p
          className={compact
            ? "absolute bottom-0 left-[calc(100%+0.625rem)] z-50 w-56 rounded-[var(--radius-control)] border border-destructive/30 bg-destructive-soft px-3 py-2 text-left text-sm leading-5 text-destructive shadow-elevated"
            : "max-w-56 text-right text-sm text-destructive"}
          role="alert"
        >
          {error}
        </p>
      ) : null}
    </div>
  );
}

function LogoutIcon() {
  return (
    <svg
      viewBox="0 0 20 20"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      className="size-4"
      aria-hidden="true"
    >
      <path d="M8 4H4.5A1.5 1.5 0 0 0 3 5.5v9A1.5 1.5 0 0 0 4.5 16H8M12 6l4 4-4 4M16 10H7" />
    </svg>
  );
}

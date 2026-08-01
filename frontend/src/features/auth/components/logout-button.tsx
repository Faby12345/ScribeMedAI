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
    <div className="flex flex-col items-end gap-2">
      <Button
        type="button"
        variant="destructive"
        size="sm"
        isLoading={isLoggingOut}
        loadingText="Se deconectează"
        onClick={handleLogout}
        className={compact ? "px-3" : "w-full"}
      >
        Deconectare
      </Button>
      {error ? (
        <p className="max-w-56 text-right text-sm text-destructive" role="alert">
          {error}
        </p>
      ) : null}
    </div>
  );
}

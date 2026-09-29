import type { ReactNode } from "react";

import type { AuthenticatedUser } from "@/features/auth/types";
import { cn } from "@/lib/class-names";

type PatientAreaShellProps = {
  user: AuthenticatedUser;
  activeItem:
    | "overview"
    | "patients"
    | "consultations"
    | "medications"
    | "knowledge"
    | "settings";
  children: ReactNode;
  width?: "standard" | "wide";
  layout?: "page" | "workspace";
};

export function PatientAreaShell({
  children,
  width = "standard",
  layout = "page",
}: PatientAreaShellProps) {
  return (
    <div
      className={cn(
        "mx-auto w-full px-5 sm:px-8 lg:px-10",
        layout === "workspace"
          ? "flex h-[calc(100dvh-4.25rem)] flex-col overflow-hidden py-4 sm:py-5"
          : "py-7 sm:py-8",
        width === "wide" ? "max-w-6xl lg:max-w-[90rem]" : "max-w-6xl",
      )}
    >
      {children}
    </div>
  );
}

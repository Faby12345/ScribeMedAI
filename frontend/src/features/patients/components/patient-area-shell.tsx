import type { ReactNode } from "react";

import type { AuthenticatedUser } from "@/features/auth/types";
import { cn } from "@/lib/class-names";

type PatientAreaShellProps = {
  user: AuthenticatedUser;
  activeItem: "overview" | "patients" | "consultations" | "medications" | "settings";
  children: ReactNode;
  width?: "standard" | "wide";
};

export function PatientAreaShell({
  children,
  width = "standard",
}: PatientAreaShellProps) {
  return (
    <div
      className={cn(
        "mx-auto w-full px-5 py-7 sm:px-8 sm:py-8 lg:px-10",
        width === "wide" ? "max-w-6xl lg:max-w-[90rem]" : "max-w-6xl",
      )}
    >
      {children}
    </div>
  );
}

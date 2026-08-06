import type { ReactNode } from "react";

import type { AuthenticatedUser } from "@/features/auth/types";

type PatientAreaShellProps = {
  user: AuthenticatedUser;
  activeItem: "overview" | "patients" | "consultations" | "settings";
  children: ReactNode;
};

export function PatientAreaShell({
  children,
}: PatientAreaShellProps) {
  return (
    <div className="mx-auto w-full max-w-6xl px-5 py-7 sm:px-8 sm:py-8 lg:px-10">
      {children}
    </div>
  );
}

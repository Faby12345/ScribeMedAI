import type { Patient } from "@/features/patients/types";
import { cn } from "@/lib/class-names";

type PatientAvatarProps = {
  patient: Pick<Patient, "firstName" | "lastName">;
  className?: string;
};

export function PatientAvatar({ patient, className }: PatientAvatarProps) {
  return (
    <span
      className={cn(
        "flex size-10 shrink-0 items-center justify-center rounded-[var(--radius-control)] border border-primary/15 bg-primary-soft text-sm font-semibold text-primary",
        className,
      )}
      aria-hidden="true"
    >
      {initialsFrom(patient)}
    </span>
  );
}

function initialsFrom(patient: Pick<Patient, "firstName" | "lastName">) {
  const firstInitial = patient.firstName.trim().charAt(0);
  const lastInitial = patient.lastName.trim().charAt(0);

  return `${lastInitial}${firstInitial}`.toLocaleUpperCase("ro-RO") || "P";
}

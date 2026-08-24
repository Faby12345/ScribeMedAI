import type { Patient } from "@/features/patients/types";
import { cn } from "@/lib/class-names";

type PatientAvatarProps = {
  patient: Pick<Patient, "firstName" | "lastName">;
  className?: string;
};

export function PatientAvatar({ className }: PatientAvatarProps) {
  return (
    <span
      className={cn(
        "flex size-10 shrink-0 items-center justify-center text-primary",
        className,
      )}
      aria-hidden="true"
    >
      <svg
        className="size-5"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      >
        <path d="M12 12.25a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z" />
        <path d="M4.75 20.25a7.25 7.25 0 0 1 14.5 0" />
      </svg>
    </span>
  );
}

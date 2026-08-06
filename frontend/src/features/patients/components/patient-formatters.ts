import type { PatientSex, PatientStatus } from "@/features/patients/types";

export const sexLabels: Record<PatientSex, string> = {
  FEMALE: "Feminin",
  MALE: "Masculin",
  OTHER: "Altul",
  UNKNOWN: "Necunoscut",
};

export const statusLabels: Record<PatientStatus, string> = {
  ACTIVE: "Activ",
  ARCHIVED: "Arhivat",
};

export function patientDisplayName({
  firstName,
  lastName,
}: {
  firstName: string;
  lastName: string;
}) {
  return `${lastName} ${firstName}`.trim();
}

export function formatDate(value: string | null) {
  if (!value) {
    return "Nespecificată";
  }

  return new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "medium",
  }).format(new Date(`${value}T00:00:00`));
}

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("ro-RO", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

export function ageFromBirthDate(value: string | null) {
  if (!value) {
    return null;
  }

  const birthDate = new Date(`${value}T00:00:00`);

  if (Number.isNaN(birthDate.getTime())) {
    return null;
  }

  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const monthDifference = today.getMonth() - birthDate.getMonth();

  if (
    monthDifference < 0 ||
    (monthDifference === 0 && today.getDate() < birthDate.getDate())
  ) {
    age -= 1;
  }

  return age;
}

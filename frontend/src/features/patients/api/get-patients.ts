import type { Patient } from "@/features/patients/types";

export class GetPatientsApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "GetPatientsApiError";
  }
}

export async function getPatients(): Promise<Patient[]> {
  let response: Response;

  try {
    response = await fetch("/api/v1/patients", {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    });
  } catch {
    throw new GetPatientsApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new GetPatientsApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new GetPatientsApiError(
      "Lista pacienților nu a putut fi încărcată.",
      response.status,
    );
  }

  return response.json();
}

export async function getPatient(id: string): Promise<Patient> {
  let response: Response;

  try {
    response = await fetch(`/api/v1/patients/${id}`, {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    });
  } catch {
    throw new GetPatientsApiError(
        "Serverul nu este disponibil. Încearcă din nou.",
    );
  }
  if (response.status === 401 || response.status === 403) {
    throw new GetPatientsApiError(
        "Sesiunea a expirat sau cererea nu este autorizată.",
        response.status,
    );
  }

  if (!response.ok) {
    throw new GetPatientsApiError(
        "Lista pacienților nu a putut fi încărcată.",
        response.status,
    );
  }

  return response.json();
}

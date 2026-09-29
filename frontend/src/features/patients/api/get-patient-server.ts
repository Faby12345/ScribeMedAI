import type { ReadonlyRequestCookies } from "next/dist/server/web/spec-extension/adapters/request-cookies";

import type { Patient } from "@/features/patients/types";

const backendUrl = process.env.SCRIBEMED_BACKEND_URL ?? "http://localhost:8080";

export class GetPatientApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "GetPatientApiError";
  }
}

export async function getPatientById(
  patientId: string,
  requestCookies: ReadonlyRequestCookies,
): Promise<Patient> {
  const response = await fetch(`${backendUrl}/api/v1/patients/${patientId}`, {
    method: "GET",
    headers: {
      Cookie: requestCookies.toString(),
    },
    cache: "no-store",
  });

  if (response.status === 401 || response.status === 403) {
    throw new GetPatientApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (response.status === 404) {
    throw new GetPatientApiError("Pacientul nu a fost găsit.", response.status);
  }

  if (!response.ok) {
    throw new GetPatientApiError(
      "Pacientul nu a putut fi încărcat.",
      response.status,
    );
  }

  return response.json();
}

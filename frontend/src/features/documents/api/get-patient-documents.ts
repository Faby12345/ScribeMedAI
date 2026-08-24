import type { ReadonlyRequestCookies } from "next/dist/server/web/spec-extension/adapters/request-cookies";

import type { PatientDocumentSummary } from "@/features/documents/types";

const backendUrl = process.env.SCRIBEMED_BACKEND_URL ?? "http://localhost:8080";

export class GetPatientDocumentsApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "GetPatientDocumentsApiError";
  }
}

export async function getPatientDocuments(
  patientId: string,
  requestCookies: ReadonlyRequestCookies,
): Promise<PatientDocumentSummary[]> {
  const response = await fetch(
    `${backendUrl}/api/v1/patients/${patientId}/documents`,
    {
      method: "GET",
      headers: {
        Cookie: requestCookies.toString(),
      },
      cache: "no-store",
    },
  );

  if (response.status === 401 || response.status === 403) {
    throw new GetPatientDocumentsApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (response.status === 404) {
    throw new GetPatientDocumentsApiError(
      "Pacientul nu a fost găsit.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new GetPatientDocumentsApiError(
      "Documentele pacientului nu au putut fi încărcate.",
      response.status,
    );
  }

  return response.json();
}

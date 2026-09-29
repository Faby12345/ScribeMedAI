import type { ReadonlyRequestCookies } from "next/dist/server/web/spec-extension/adapters/request-cookies";

import type { Consultation } from "@/features/consultations/types";

const backendUrl = process.env.SCRIBEMED_BACKEND_URL ?? "http://localhost:8080";

export class GetConsultationApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "GetConsultationApiError";
  }
}

export async function getConsultation(
  consultationId: string,
  requestCookies: ReadonlyRequestCookies,
): Promise<Consultation> {
  const response = await fetch(
    `${backendUrl}/api/v1/consultations/${consultationId}`,
    {
      method: "GET",
      headers: {
        Cookie: requestCookies.toString(),
      },
      cache: "no-store",
    },
  );

  if (response.status === 401 || response.status === 403) {
    throw new GetConsultationApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (response.status === 404) {
    throw new GetConsultationApiError(
      "Consultația nu a fost găsită.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new GetConsultationApiError(
      "Consultația nu a putut fi încărcată.",
      response.status,
    );
  }

  return response.json();
}

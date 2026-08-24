import type { ReadonlyRequestCookies } from "next/dist/server/web/spec-extension/adapters/request-cookies";

import type { DocumentReviewDocument } from "@/features/documents/types";

const backendUrl = process.env.SCRIBEMED_BACKEND_URL ?? "http://localhost:8080";

export class GetDocumentReviewApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "GetDocumentReviewApiError";
  }
}

export async function getDocumentReview(
  consultationId: string,
  requestCookies: ReadonlyRequestCookies,
): Promise<DocumentReviewDocument> {
  const response = await fetch(
    `${backendUrl}/api/v1/consultations/${consultationId}/document`,
    {
      method: "GET",
      headers: {
        Cookie: requestCookies.toString(),
      },
      cache: "no-store",
    },
  );

  if (response.status === 401 || response.status === 403) {
    throw new GetDocumentReviewApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (response.status === 404) {
    throw new GetDocumentReviewApiError(
      "Draftul clinic nu este disponibil încă.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new GetDocumentReviewApiError(
      "Draftul clinic nu a putut fi încărcat.",
      response.status,
    );
  }

  return response.json();
}

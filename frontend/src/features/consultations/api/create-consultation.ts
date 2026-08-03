import type { Consultation } from "@/features/consultations/types";

export class CreateConsultationApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "CreateConsultationApiError";
  }
}

type CsrfTokenResponse = {
  headerName: string;
  parameterName: string;
  token: string;
};

export async function createConsultation(
  patientId: string,
): Promise<Consultation> {
  let response: Response;

  try {
    const csrfToken = await getCsrfToken();
    response = await fetch("/api/v1/consultations", {
      method: "POST",
      headers: createHeaders(csrfToken),
      credentials: "include",
      body: JSON.stringify({ patientId }),
    });
  } catch (error) {
    if (error instanceof CreateConsultationApiError) {
      throw error;
    }

    throw new CreateConsultationApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new CreateConsultationApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (response.status === 400) {
    throw new CreateConsultationApiError(
      "Consultația nu poate fi creată pentru pacientul selectat.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new CreateConsultationApiError(
      "Consultația nu a putut fi creată. Încearcă din nou.",
      response.status,
    );
  }

  return response.json();
}

async function getCsrfToken(): Promise<CsrfTokenResponse> {
  const response = await fetch("/api/v1/csrf", {
    method: "GET",
    credentials: "include",
    cache: "no-store",
  });

  if (!response.ok) {
    throw new CreateConsultationApiError(
      "Nu am putut pregăti cererea securizată. Încearcă din nou.",
      response.status,
    );
  }

  return response.json();
}

function createHeaders(csrfToken: CsrfTokenResponse) {
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    [csrfToken.headerName]: csrfToken.token,
  };

  return headers;
}

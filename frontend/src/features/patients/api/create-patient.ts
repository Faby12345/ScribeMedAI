import type {
  CreatePatientRequest,
  Patient,
} from "@/features/patients/types";

export class CreatePatientApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "CreatePatientApiError";
  }
}

type CsrfTokenResponse = {
  headerName: string;
  parameterName: string;
  token: string;
};

export async function createPatient(
  request: CreatePatientRequest,
): Promise<Patient> {
  let response: Response;

  try {
    const csrfToken = await getCsrfToken();
    response = await fetch("/api/v1/patients", {
      method: "POST",
      headers: createHeaders(csrfToken),
      credentials: "include",
      body: JSON.stringify(request),
    });
  } catch (error) {
    if (error instanceof CreatePatientApiError) {
      throw error;
    }

    throw new CreatePatientApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new CreatePatientApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (response.status === 400) {
    throw new CreatePatientApiError(
      "Verifică datele pacientului și încearcă din nou.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new CreatePatientApiError(
      "Pacientul nu a putut fi salvat. Încearcă din nou.",
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
    throw new CreatePatientApiError(
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

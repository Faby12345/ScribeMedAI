import { getCsrfToken } from "@/features/consultations/api/create-consultation";

export class ConfirmPatientInformedApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "ConfirmPatientInformedApiError";
  }
}

export async function confirmPatientInformed(
  consultationId: string,
): Promise<void> {
  let response: Response;

  try {
    const csrfToken = await getCsrfToken();
    response = await fetch(
      `/api/v1/consultations/${consultationId}/patient-informed`,
      {
        method: "POST",
        headers: {
          [csrfToken.headerName]: csrfToken.token,
        },
        credentials: "include",
      },
    );
  } catch (error) {
    if (error instanceof ConfirmPatientInformedApiError) {
      throw error;
    }

    throw new ConfirmPatientInformedApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new ConfirmPatientInformedApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new ConfirmPatientInformedApiError(
      "Confirmarea informării pacientului nu a putut fi salvată. Încearcă din nou.",
      response.status,
    );
  }
}

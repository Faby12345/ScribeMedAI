import type { PaginatedResponse } from "@/features/consultations/types";
import type { MedicationResponse } from "@/features/medication/types";

export class GetMedicationApiError extends Error {
    constructor(
        message: string,
        public readonly status?: number,
    ) {
        super(message);
        this.name = "GetMedicationApiError";
    }
}

export async function getMedication({
  page,
  size,
}: {
  page: number;
  size: number;
}): Promise<PaginatedResponse<MedicationResponse>> {
  let response: Response;

  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  try {
    response = await fetch(`/api/v1/medications?${searchParams}`, {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    });
  } catch {
    throw new GetMedicationApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new GetMedicationApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new GetMedicationApiError(
      "Lista medicamentelor nu a putut fi încărcată.",
      response.status,
    );
  }

  return response.json();
}

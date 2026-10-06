import type { MedicationResponse } from "@/features/medication/types";

export class SearchMedicationsApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "SearchMedicationsApiError";
  }
}

export async function searchMedications(
  query: string,
  signal?: AbortSignal,
): Promise<MedicationResponse[]> {
  let response: Response;

  try {
    response = await fetch(
      `/api/v1/medications/query?query=${encodeURIComponent(query)}`,
      {
        credentials: "include",
        cache: "no-store",
        signal,
      },
    );
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw error;
    }

    throw new SearchMedicationsApiError(
      "Căutarea nu este disponibilă momentan. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new SearchMedicationsApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new SearchMedicationsApiError(
      "Medicamentele nu au putut fi căutate. Încearcă din nou.",
      response.status,
    );
  }

  return response.json();
}

import type {
  Consultation,
  PaginatedResponse,
} from "@/features/consultations/types";

export class GetConsultationsApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "GetConsultationsApiError";
  }
}

export async function getConsultations({
  page,
  size,
}: {
  page: number;
  size: number;
}): Promise<PaginatedResponse<Consultation>> {
  let response: Response;
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  try {
    response = await fetch(`/api/v1/consultations?${searchParams}`, {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    });
  } catch {
    throw new GetConsultationsApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new GetConsultationsApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new GetConsultationsApiError(
      "Lista consultațiilor nu a putut fi încărcată.",
      response.status,
    );
  }

  return response.json();
}

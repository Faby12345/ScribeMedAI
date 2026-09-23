import type { PaginatedResponse } from "@/features/consultations/types";
import type { KnowledgeDocument } from "@/features/knowledge/types";

export class GetKnowledgeDocumentsApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "GetKnowledgeDocumentsApiError";
  }
}

export async function getKnowledgeDocuments({
  page,
  size,
}: {
  page: number;
  size: number;
}): Promise<PaginatedResponse<KnowledgeDocument>> {
  let response: Response;
  const searchParams = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  try {
    response = await fetch(`/api/v1/knowledge?${searchParams}`, {
      method: "GET",
      credentials: "include",
      cache: "no-store",
    });
  } catch {
    throw new GetKnowledgeDocumentsApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new GetKnowledgeDocumentsApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new GetKnowledgeDocumentsApiError(
      "Lista documentelor nu a putut fi încărcată.",
      response.status,
    );
  }

  const documentsPage: unknown = await response.json();

  if (!isPaginatedKnowledgeResponse(documentsPage)) {
    throw new GetKnowledgeDocumentsApiError(
      "Serverul a returnat un răspuns neașteptat.",
      response.status,
    );
  }

  return documentsPage;
}

function isPaginatedKnowledgeResponse(
  value: unknown,
): value is PaginatedResponse<KnowledgeDocument> {
  if (!value || typeof value !== "object") {
    return false;
  }

  const page = value as Partial<PaginatedResponse<KnowledgeDocument>>;

  return (
    Array.isArray(page.content) &&
    typeof page.totalElements === "number" &&
    typeof page.totalPages === "number" &&
    typeof page.size === "number" &&
    typeof page.number === "number" &&
    typeof page.first === "boolean" &&
    typeof page.last === "boolean"
  );
}

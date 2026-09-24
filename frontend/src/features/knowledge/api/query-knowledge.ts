import { getCsrfToken } from "@/features/consultations/api/create-consultation";
import type { KnowledgeQueryResponse } from "@/features/knowledge/types";

export class QueryKnowledgeApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "QueryKnowledgeApiError";
  }
}

export async function queryKnowledge(
  query: string,
  documentIds: string[],
): Promise<KnowledgeQueryResponse> {
  let response: Response;

  try {
    const csrfToken = await getCsrfToken();
    response = await fetch("/api/v1/knowledge/query", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        [csrfToken.headerName]: csrfToken.token,
      },
      credentials: "include",
      body: JSON.stringify({
        query,
        documentsIds: documentIds,
      }),
    });
  } catch (error) {
    if (error instanceof QueryKnowledgeApiError) {
      throw error;
    }

    throw new QueryKnowledgeApiError(
      "Serverul nu este disponibil. Verifică conexiunea și încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new QueryKnowledgeApiError(
      "Sesiunea a expirat. Autentifică-te din nou pentru a continua.",
      response.status,
    );
  }

  if (response.status === 400) {
    throw new QueryKnowledgeApiError(
      "Întrebarea sau selecția documentelor nu este validă.",
      response.status,
    );
  }

  if (response.status === 429) {
    throw new QueryKnowledgeApiError(
      "Ai trimis prea multe întrebări într-un timp scurt. Așteaptă puțin și reîncearcă.",
      response.status,
    );
  }

  if (response.status === 502 || response.status === 503) {
    throw new QueryKnowledgeApiError(
      "Serviciul de răspuns nu este disponibil momentan. Încearcă din nou.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new QueryKnowledgeApiError(
      "Răspunsul nu a putut fi generat. Încearcă din nou.",
      response.status,
    );
  }

  const result: unknown = await response.json();
  if (!isKnowledgeQueryResponse(result)) {
    throw new QueryKnowledgeApiError(
      "Serverul a returnat un răspuns neașteptat.",
      response.status,
    );
  }

  return result;
}

function isKnowledgeQueryResponse(value: unknown): value is KnowledgeQueryResponse {
  if (!value || typeof value !== "object") {
    return false;
  }

  const response = value as Partial<KnowledgeQueryResponse>;
  return typeof response.answer === "string" && Array.isArray(response.citations);
}

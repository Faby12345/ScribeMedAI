import { createHeaders, getCsrfToken } from "@/features/consultations/api/create-consultation";
import type { DocumentReviewDocument, SoapDraft } from "@/features/documents/types";

export class SaveDocumentDraftApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "SaveDocumentDraftApiError";
  }
}

export async function saveDocumentDraft({
  documentId,
  draft,
  reviewFlags,
}: {
  documentId: string;
  draft: SoapDraft;
  reviewFlags: string[];
}): Promise<DocumentReviewDocument> {
  let response: Response;

  try {
    const csrfToken = await getCsrfToken();

    response = await fetch(`/api/v1/documents/${documentId}/draft`, {
      method: "PATCH",
      headers: createHeaders(csrfToken),
      credentials: "include",
      body: JSON.stringify({ draft, reviewFlags }),
    });
  } catch (error) {
    if (error instanceof SaveDocumentDraftApiError) {
      throw error;
    }

    throw new SaveDocumentDraftApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new SaveDocumentDraftApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (response.status === 409) {
    throw new SaveDocumentDraftApiError(
      "Documentul nu mai poate fi editat în starea curentă.",
      response.status,
    );
  }

  if (response.status === 400) {
    throw new SaveDocumentDraftApiError(
      "Draftul conține date invalide. Verifică secțiunile și încearcă din nou.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new SaveDocumentDraftApiError(
      "Draftul nu a putut fi salvat. Încearcă din nou.",
      response.status,
    );
  }

  return response.json();
}

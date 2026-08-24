import { createHeaders, getCsrfToken } from "@/features/consultations/api/create-consultation";

export class ApproveDocumentApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "ApproveDocumentApiError";
  }
}

export type DocumentApprovalResponse = {
  documentId: string;
  documentStatus: "DRAFT" | "APPROVED" | "ARCHIVED";
  versionId: string;
  versionNumber: number;
  versionStatus: "DRAFT" | "APPROVED" | "SUPERSEDED";
  approvedAt: string;
};

export async function approveDocument(
  documentId: string,
  versionId: string,
): Promise<DocumentApprovalResponse> {
  let response: Response;

  try {
    const csrfToken = await getCsrfToken();

    response = await fetch(`/api/v1/documents/${documentId}/approval`, {
      method: "POST",
      headers: createHeaders(csrfToken),
      credentials: "include",
      body: JSON.stringify({
        versionId,
        doctorReviewedAndApproved: true,
      }),
    });
  } catch (error) {
    if (error instanceof ApproveDocumentApiError) {
      throw error;
    }

    throw new ApproveDocumentApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new ApproveDocumentApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (response.status === 409) {
    throw new ApproveDocumentApiError(
      "Draftul a fost modificat între timp. Reîncarcă pagina înainte de aprobare.",
      response.status,
    );
  }

  if (response.status === 400) {
    throw new ApproveDocumentApiError(
      "Documentul nu poate fi aprobat fără confirmarea revizuirii.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new ApproveDocumentApiError(
      "Documentul nu a putut fi aprobat. Încearcă din nou.",
      response.status,
    );
  }

  return response.json();
}

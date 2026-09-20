import {getCsrfToken} from "@/features/consultations/api/create-consultation";

export class UploadPdfApiError extends Error {
    constructor(
        message: string,
        public readonly status?: number,
    ) {
        super(message);
        this.name = "UploadPdfApiError"
    }
}


type KnowledgeDocumentRequest = {
    title: string,
    sourceInstitution: string,
    sourceUrl?: string,
    publishedAt?: string,
    version?: string,
}

export async function uploadKnowledgeDocument(
    file: File,
    request: KnowledgeDocumentRequest
) : Promise<void> {
    let response: Response;
    try {
        const csrfToken = await getCsrfToken();
        const formData = new FormData();
        formData.append("file", file);
        formData.append(
            "request",
            new Blob([JSON.stringify(request)], {
                type: "application/json",
            }),
        );

        response = await fetch("/api/v1/pdf/upload", {
            method: "POST",
            headers: {
                [csrfToken.headerName]: csrfToken.token,
            },
            credentials: "include",
            body: formData
        });

    } catch (error) {
        if (error instanceof UploadPdfApiError) {
            throw error;
        }

        throw new UploadPdfApiError(
            "Serverul nu este disponibil. Încearcă din nou.",
        );
    }

    if (response.status === 401 || response.status === 403) {
        throw new UploadPdfApiError(
            "Sesiunea a expirat sau cererea nu este autorizată.",
            response.status,
        );
    }

    if (response.status === 400) {
        throw new UploadPdfApiError(
            "Documentul nu poate fi trimis. Verifică formatul și dimensiunea fișierului.",
            response.status,
        );
    }

    if (!response.ok) {
        throw new UploadPdfApiError(
            "Documentul nu a putut fi trimis. Încearcă din nou.",
            response.status,
        );
    }

}

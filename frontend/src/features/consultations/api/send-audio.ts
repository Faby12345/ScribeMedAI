import { getCsrfToken } from "@/features/consultations/api/create-consultation";

export class SendAudioApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "SendAudioApiError";
  }
}

type SendAudioResponse = {
  audioId: string;
  jobId: string;
  consultationId: string;
  status: string;
};

export async function sendAudio(
  consultationId: string,
  file: File,
): Promise<SendAudioResponse> {
  let response: Response;

  try {
    const csrfToken = await getCsrfToken();
    const formData = new FormData();
    formData.append("file", file);

    response = await fetch(`/api/v1/consultations/${consultationId}/audio`, {
      method: "POST",
      headers: {
        [csrfToken.headerName]: csrfToken.token,
      },
      credentials: "include",
      body: formData,
    });
  } catch (error) {
    if (error instanceof SendAudioApiError) {
      throw error;
    }

    throw new SendAudioApiError(
      "Serverul nu este disponibil. Încearcă din nou.",
    );
  }

  if (response.status === 401 || response.status === 403) {
    throw new SendAudioApiError(
      "Sesiunea a expirat sau cererea nu este autorizată.",
      response.status,
    );
  }

  if (response.status === 400) {
    throw new SendAudioApiError(
      "Fișierul audio nu poate fi trimis. Verifică formatul și dimensiunea fișierului.",
      response.status,
    );
  }

  if (!response.ok) {
    throw new SendAudioApiError(
      "Audio-ul nu a putut fi trimis. Încearcă din nou.",
      response.status,
    );
  }

  return response.json();
}

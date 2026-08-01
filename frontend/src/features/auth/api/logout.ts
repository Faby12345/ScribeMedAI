export class LogoutApiError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "LogoutApiError";
  }
}

export async function logout(): Promise<void> {
  let response: Response;

  try {
    response = await fetch("/api/v1/auth/logout", {
      method: "POST",
      credentials: "include",
    });
  } catch {
    throw new LogoutApiError("Serverul nu este disponibil. Încearcă din nou.");
  }

  if (!response.ok) {
    throw new LogoutApiError("Deconectarea nu a reușit. Încearcă din nou.");
  }
}

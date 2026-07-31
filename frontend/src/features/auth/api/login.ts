import type { LoginRequest, LoginResponse } from "@/features/auth/types";

export class LoginApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message);
    this.name = "LoginApiError";
  }
}

export async function login(request: LoginRequest): Promise<LoginResponse> {
  let response: Response;

  try {
    response = await fetch("/api/v1/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify(request),
    });
  } catch {
    throw new LoginApiError("Serverul nu este disponibil. Încearcă din nou.");
  }

  if (response.status === 401) {
    throw new LoginApiError("Emailul sau parola este incorectă.", response.status);
  }

  if (!response.ok) {
    throw new LoginApiError(
      "Autentificarea nu a reușit. Încearcă din nou.",
      response.status,
    );
  }

  return response.json();
}

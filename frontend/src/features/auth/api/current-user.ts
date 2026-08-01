import type { ReadonlyRequestCookies } from "next/dist/server/web/spec-extension/adapters/request-cookies";

import type { AuthenticatedUser } from "@/features/auth/types";

const backendUrl = process.env.SCRIBEMED_BACKEND_URL ?? "http://localhost:8080";

export async function getCurrentUser(
  requestCookies: ReadonlyRequestCookies,
): Promise<AuthenticatedUser | null> {
  const response = await fetch(`${backendUrl}/api/v1/auth/me`, {
    method: "GET",
    headers: {
      Cookie: requestCookies.toString(),
    },
    cache: "no-store",
  });

  if (response.status === 401 || response.status === 403) {
    return null;
  }

  if (!response.ok) {
    throw new Error("Nu am putut verifica sesiunea curentă.");
  }

  return response.json();
}

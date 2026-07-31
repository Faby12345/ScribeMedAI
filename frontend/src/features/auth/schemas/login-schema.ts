import type { LoginRequest } from "@/features/auth/types";

export type LoginFormValues = LoginRequest;

export type LoginFormErrors = Partial<Record<keyof LoginFormValues, string>>;

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateLoginForm(values: LoginFormValues): LoginFormErrors {
  const errors: LoginFormErrors = {};
  const email = values.email.trim();

  if (!email) {
    errors.email = "Introdu adresa de email.";
  } else if (!emailPattern.test(email)) {
    errors.email = "Introdu o adresă de email validă.";
  }

  if (!values.password) {
    errors.password = "Introdu parola.";
  }

  return errors;
}

export function hasLoginFormErrors(errors: LoginFormErrors) {
  return Object.values(errors).some(Boolean);
}

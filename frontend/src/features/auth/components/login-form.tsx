"use client";

import { useRouter } from "next/navigation";
import { useId, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { Input } from "@/components/ui/input";
import { LoginApiError, login } from "@/features/auth/api/login";
import {
  hasLoginFormErrors,
  type LoginFormErrors,
  type LoginFormValues,
  validateLoginForm,
} from "@/features/auth/schemas/login-schema";

const emptyValues: LoginFormValues = {
  email: "",
  password: "",
};

type LoginFormProps = {
  initialValues?: Partial<LoginFormValues>;
};

export function LoginForm({ initialValues }: LoginFormProps) {
  const router = useRouter();
  const emailId = useId();
  const passwordId = useId();
  const [values, setValues] = useState<LoginFormValues>({
    ...emptyValues,
    ...initialValues,
  });
  const [errors, setErrors] = useState<LoginFormErrors>({});
  const [authError, setAuthError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    const nextErrors = validateLoginForm(values);
    setErrors(nextErrors);
    setAuthError(null);

    if (hasLoginFormErrors(nextErrors)) {
      return;
    }

    setIsSubmitting(true);

    try {
      await login({
        email: values.email.trim(),
        password: values.password,
      });
      router.replace("/dashboard");
      router.refresh();
    } catch (error) {
      if (error instanceof LoginApiError) {
        setAuthError(error.message);
      } else {
        setAuthError("Autentificarea nu a reușit. Încearcă din nou.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  function updateField(field: keyof LoginFormValues, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setAuthError(null);
  }

  return (
    <form className="space-y-5" noValidate onSubmit={handleSubmit}>
      {authError ? (
        <Alert variant="error" title="Autentificarea a eșuat">
          {authError}
        </Alert>
      ) : null}

      <FormField id={emailId} label="Adresă de email" error={errors.email}>
        <Input
          id={emailId}
          name="email"
          type="email"
          autoComplete="email"
          inputMode="email"
          value={values.email}
          onChange={(event) => updateField("email", event.target.value)}
          hasError={Boolean(errors.email)}
          aria-describedby={errors.email ? `${emailId}-error` : undefined}
          disabled={isSubmitting}
        />
      </FormField>

      <FormField id={passwordId} label="Parolă" error={errors.password}>
        <Input
          id={passwordId}
          name="password"
          type={showPassword ? "text" : "password"}
          autoComplete="current-password"
          value={values.password}
          onChange={(event) => updateField("password", event.target.value)}
          hasError={Boolean(errors.password)}
          aria-describedby={errors.password ? `${passwordId}-error` : undefined}
          disabled={isSubmitting}
          trailingIcon={
            <button
              type="button"
              className="rounded px-2 py-1 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              onClick={() => setShowPassword((current) => !current)}
              aria-label={showPassword ? "Ascunde parola" : "Afișează parola"}
              aria-pressed={showPassword}
              disabled={isSubmitting}
            >
              {showPassword ? "Ascunde" : "Afișează"}
            </button>
          }
        />
      </FormField>

      <div className="flex items-center justify-between gap-4 text-sm">
        <span className="text-muted-foreground">
          Sesiune securizată prin cookie.
        </span>
        <a
          className="font-medium text-primary underline-offset-4 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          href="mailto:support@scribemed.ai"
        >
          Ai uitat parola?
        </a>
      </div>

      <Button
        type="submit"
        size="lg"
        className="w-full"
        isLoading={isSubmitting}
        loadingText="Se autentifică"
      >
        Autentifică-te
      </Button>
    </form>
  );
}

"use client";

import { type FormEvent, useId, useState } from "react";

import { Alert } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { FormField } from "@/components/ui/form-field";
import { Input, Select } from "@/components/ui/input";
import {
  CreatePatientApiError,
  createPatient,
} from "@/features/patients/api/create-patient";
import {
  emptyCreatePatientValues,
  hasCreatePatientFormErrors,
  toCreatePatientRequest,
  type CreatePatientFormErrors,
  type CreatePatientFormValues,
  validateCreatePatientForm,
} from "@/features/patients/schemas/create-patient-schema";
import type { Patient } from "@/features/patients/types";

type CreatePatientFormProps = {
  onCancel: () => void;
  onCreated: (patient: Patient) => void;
};

export function CreatePatientForm({
  onCancel,
  onCreated,
}: CreatePatientFormProps) {
  const firstNameId = useId();
  const lastNameId = useId();
  const birthDateId = useId();
  const sexId = useId();
  const phoneId = useId();
  const emailId = useId();
  const [values, setValues] = useState<CreatePatientFormValues>(
    emptyCreatePatientValues,
  );
  const [errors, setErrors] = useState<CreatePatientFormErrors>({});
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (isSubmitting) {
      return;
    }

    const nextErrors = validateCreatePatientForm(values);
    setErrors(nextErrors);
    setSubmitError(null);

    if (hasCreatePatientFormErrors(nextErrors)) {
      return;
    }

    setIsSubmitting(true);

    try {
      const patient = await createPatient(toCreatePatientRequest(values));
      onCreated(patient);
    } catch (error) {
      if (error instanceof CreatePatientApiError) {
        setSubmitError(error.message);
      } else {
        setSubmitError("Pacientul nu a putut fi salvat. Încearcă din nou.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  function updateField(field: keyof CreatePatientFormValues, value: string) {
    setValues((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setSubmitError(null);
  }

  return (
    <form className="flex min-h-0 flex-1 flex-col" noValidate onSubmit={handleSubmit}>
      <div className="min-h-0 flex-1 overflow-y-auto px-5 py-5 sm:px-6">
        <div className="space-y-5">
          {submitError ? (
            <Alert variant="error" title="Salvarea a eșuat">
              {submitError}
            </Alert>
          ) : null}

          <section aria-labelledby="patient-details-title">
            <div className="mb-4">
              <h3 id="patient-details-title" className="section-title">
                Date pacient
              </h3>
              <p className="secondary-text mt-1">
                Completează doar informațiile necesare pentru începerea
                consultației.
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <FormField
                id={firstNameId}
                label="Prenume"
                error={errors.firstName}
                required
              >
                <Input
                  id={firstNameId}
                  name="firstName"
                  autoComplete="given-name"
                  value={values.firstName}
                  onChange={(event) =>
                    updateField("firstName", event.target.value)
                  }
                  hasError={Boolean(errors.firstName)}
                  aria-describedby={
                    errors.firstName ? `${firstNameId}-error` : undefined
                  }
                  disabled={isSubmitting}
                />
              </FormField>

              <FormField
                id={lastNameId}
                label="Nume"
                error={errors.lastName}
                required
              >
                <Input
                  id={lastNameId}
                  name="lastName"
                  autoComplete="family-name"
                  value={values.lastName}
                  onChange={(event) =>
                    updateField("lastName", event.target.value)
                  }
                  hasError={Boolean(errors.lastName)}
                  aria-describedby={
                    errors.lastName ? `${lastNameId}-error` : undefined
                  }
                  disabled={isSubmitting}
                />
              </FormField>

              <FormField
                id={birthDateId}
                label="Data nașterii"
                error={errors.birthDate}
              >
                <Input
                  id={birthDateId}
                  name="birthDate"
                  type="date"
                  value={values.birthDate}
                  onChange={(event) =>
                    updateField("birthDate", event.target.value)
                  }
                  hasError={Boolean(errors.birthDate)}
                  aria-describedby={
                    errors.birthDate ? `${birthDateId}-error` : undefined
                  }
                  disabled={isSubmitting}
                />
              </FormField>

              <FormField id={sexId} label="Sex">
                <Select
                  id={sexId}
                  name="sex"
                  value={values.sex}
                  onChange={(event) => updateField("sex", event.target.value)}
                  disabled={isSubmitting}
                >
                  <option value="">Nespecificat</option>
                  <option value="FEMALE">Feminin</option>
                  <option value="MALE">Masculin</option>
                  <option value="OTHER">Altul</option>
                  <option value="UNKNOWN">Necunoscut</option>
                </Select>
              </FormField>

              <FormField id={phoneId} label="Telefon" error={errors.phone}>
                <Input
                  id={phoneId}
                  name="phone"
                  type="tel"
                  autoComplete="tel"
                  inputMode="tel"
                  value={values.phone}
                  onChange={(event) => updateField("phone", event.target.value)}
                  hasError={Boolean(errors.phone)}
                  aria-describedby={errors.phone ? `${phoneId}-error` : undefined}
                  disabled={isSubmitting}
                />
              </FormField>

              <FormField id={emailId} label="Email" error={errors.email}>
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
            </div>
          </section>
        </div>
      </div>

      <div className="border-t border-border bg-surface px-5 py-4 sm:px-6">
        <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={onCancel}
            disabled={isSubmitting}
          >
            Anulează
          </Button>
          <Button
            type="submit"
            variant="primary"
            isLoading={isSubmitting}
            loadingText="Se salvează"
          >
            Salvează pacientul
          </Button>
        </div>
      </div>
    </form>
  );
}

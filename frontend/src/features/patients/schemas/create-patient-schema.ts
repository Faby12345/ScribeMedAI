import type {
  CreatePatientRequest,
  PatientSex,
} from "@/features/patients/types";

export type CreatePatientFormValues = {
  firstName: string;
  lastName: string;
  birthDate: string;
  sex: "" | PatientSex;
  phone: string;
  email: string;
};

export type CreatePatientFormErrors = Partial<
  Record<keyof CreatePatientFormValues, string>
>;

export const emptyCreatePatientValues: CreatePatientFormValues = {
  firstName: "",
  lastName: "",
  birthDate: "",
  sex: "",
  phone: "",
  email: "",
};

export function validateCreatePatientForm(
  values: CreatePatientFormValues,
): CreatePatientFormErrors {
  const errors: CreatePatientFormErrors = {};

  if (!values.firstName.trim()) {
    errors.firstName = "Prenumele pacientului este obligatoriu.";
  } else if (values.firstName.trim().length > 100) {
    errors.firstName = "Prenumele pacientului nu poate depăși 100 de caractere.";
  }

  if (!values.lastName.trim()) {
    errors.lastName = "Numele pacientului este obligatoriu.";
  } else if (values.lastName.trim().length > 100) {
    errors.lastName = "Numele pacientului nu poate depăși 100 de caractere.";
  }

  if (values.birthDate) {
    const birthDate = new Date(`${values.birthDate}T00:00:00`);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (Number.isNaN(birthDate.getTime())) {
      errors.birthDate = "Data nașterii nu este validă.";
    } else if (birthDate > today) {
      errors.birthDate = "Data nașterii nu poate fi în viitor.";
    }
  }

  if (values.phone.trim().length > 50) {
    errors.phone = "Numărul de telefon nu poate depăși 50 de caractere.";
  }

  if (values.email.trim()) {
    if (values.email.trim().length > 320) {
      errors.email = "Adresa de email nu poate depăși 320 de caractere.";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
      errors.email = "Adresa de email nu este validă.";
    }
  }

  return errors;
}

export function hasCreatePatientFormErrors(errors: CreatePatientFormErrors) {
  return Object.values(errors).some(Boolean);
}

export function toCreatePatientRequest(
  values: CreatePatientFormValues,
): CreatePatientRequest {
  return {
    firstName: values.firstName.trim(),
    lastName: values.lastName.trim(),
    birthDate: values.birthDate || null,
    sex: values.sex || null,
    phone: values.phone.trim() || null,
    email: values.email.trim() || null,
  };
}

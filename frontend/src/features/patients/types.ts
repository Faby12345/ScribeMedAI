export type PatientSex = "FEMALE" | "MALE" | "OTHER" | "UNKNOWN";

export type PatientStatus = "ACTIVE" | "ARCHIVED";

export type CreatePatientRequest = {
  firstName: string;
  lastName: string;
  birthDate?: string | null;
  sex?: PatientSex | null;
  phone?: string | null;
  email?: string | null;
};

export type Patient = {
  id: string;
  tenantId: string;
  firstName: string;
  lastName: string;
  birthDate: string | null;
  sex: PatientSex | null;
  phone: string | null;
  email: string | null;
  status: PatientStatus;
  createdAt: string;
  updatedAt: string;
};

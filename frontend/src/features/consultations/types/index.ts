export type ConsultationStatus =
  | "CREATED"
  | "PATIENT_INFORMED"
  | "AUDIO_UPLOADED"
  | "TRANSCRIBING"
  | "TRANSCRIPTION_READY"
  | "TRANSCRIPTION_FAILED"
  | "NOTES_PROCESSING"
  | "NOTES_READY"
  | "NOTES_FAILED";

export type Consultation = {
  id: string;
  tenantId: string;
  patientId: string;
  patientFirstName: string;
  patientLastName: string;
  doctorUserId: string;
  status: ConsultationStatus;
  patientInformedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type PaginatedResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
};

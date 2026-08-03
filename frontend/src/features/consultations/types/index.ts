export type ConsultationStatus =
  | "CREATED"
  | "PATIENT_INFORMED"
  | "AUDIO_UPLOADED"
  | "TRANSCRIBING"
  | "TRANSCRIPTION_READY"
  | "TRANSCRIPTION_FAILED";

export type Consultation = {
  id: string;
  tenantId: string;
  patientId: string;
  doctorUserId: string;
  status: ConsultationStatus;
  patientInformedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

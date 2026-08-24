export type SoapSectionId = "subjective" | "objective" | "assessment" | "plan";

export type SoapDraft = Record<SoapSectionId, string>;

export type DocumentReviewDocument = {
  consultationId: string;
  documentId: string;
  documentType: string;
  documentStatus: "DRAFT" | "APPROVED" | "ARCHIVED";
  versionId: string;
  versionNumber: number;
  versionStatus: "DRAFT" | "APPROVED" | "SUPERSEDED";
  source: "AI_GENERATED" | "DOCTOR_EDITED" | "IMPORTED";
  draft: SoapDraft;
  reviewFlags: string[];
  aiProvider: string | null;
  aiModel: string | null;
  promptVersion: string | null;
  templateVersion: string | null;
  versionCreatedAt: string | null;
  transcript: {
    provider: string;
    model: string;
    language: string | null;
    transcriptText: string;
    createdAt: string | null;
  };
};

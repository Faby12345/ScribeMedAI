package ro.scribemed.backend.consultation.domain;

public enum ConsultationStatus {
    CREATED,
    PATIENT_INFORMED,
    AUDIO_UPLOADED,
    TRANSCRIBING,
    TRANSCRIPTION_READY,
    TRANSCRIPTION_FAILED,

    NOTES_PROCESSING,
    NOTES_READY,
    NOTES_FAILED
}

package ro.scribemed.backend.shared.rateLimit.domain;

public enum RateLimitScope {
    LOGIN_BY_IP,
    TRANSCRIPTION_BY_USER,
    LLM_GENERATION_BY_USER,
    PATIENT_READS_BY_USER,
    AUDIO_UPLOAD_BY_USER
}

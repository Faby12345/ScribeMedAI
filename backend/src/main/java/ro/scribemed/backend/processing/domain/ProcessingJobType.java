package ro.scribemed.backend.processing.domain;

public enum ProcessingJobType {
    TRANSCRIPTION(ProcessingJobTargetType.CONSULTATION),
    STRUCTURE_NOTES(ProcessingJobTargetType.CONSULTATION),
    STRUCTURE_TRANSCRIPTION(ProcessingJobTargetType.CONSULTATION),
    INGEST_DOCUMENT(ProcessingJobTargetType.KNOWLEDGE_DOCUMENT);

    private final ProcessingJobTargetType targetType;

    ProcessingJobType(ProcessingJobTargetType targetType) {
        this.targetType = targetType;
    }

    public ProcessingJobTargetType targetType() {
        return targetType;
    }
}

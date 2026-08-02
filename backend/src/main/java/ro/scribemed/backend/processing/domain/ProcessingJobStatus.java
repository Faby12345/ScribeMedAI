package ro.scribemed.backend.processing.domain;

public enum ProcessingJobStatus {
    PENDING,
    RUNNING,
    RETRY,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

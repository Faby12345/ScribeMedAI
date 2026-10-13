package ro.scribemed.backend.medicationdraft.domain;

public enum MedicationReviewStatus {
    PENDING_REVIEW,
    ACCEPTED_AS_IS,
    ACCEPTED_WITH_CHANGES,
    REJECTED,
    UNRESOLVED
}

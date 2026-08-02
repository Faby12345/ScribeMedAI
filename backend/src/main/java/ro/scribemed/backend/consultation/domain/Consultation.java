package ro.scribemed.backend.consultation.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import ro.scribemed.backend.identity.domain.AppUser;
import ro.scribemed.backend.patient.domain.Patient;
import ro.scribemed.backend.tenancy.domain.Tenant;

@Entity
@Table(name = "consultation")
public class Consultation {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_user_id", nullable = false)
    private AppUser doctorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ConsultationStatus status;

    @Column
    private Instant patientInformedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Consultation() {
    }

    public Consultation(Tenant tenant, Patient patient, AppUser doctorUser) {
        this.tenant = tenant;
        this.patient = patient;
        this.doctorUser = doctorUser;
        this.status = ConsultationStatus.CREATED;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void markPatientInformed(Instant informedAt) {
        patientInformedAt = informedAt;
        status = ConsultationStatus.PATIENT_INFORMED;
    }

    public void markAudioUploaded() {
        status = ConsultationStatus.AUDIO_UPLOADED;
    }

    public void markTranscribing() {
        status = ConsultationStatus.TRANSCRIBING;
    }

    public void markTranscriptionReady() {
        status = ConsultationStatus.TRANSCRIPTION_READY;
    }

    public void markTranscriptionFailed() {
        status = ConsultationStatus.TRANSCRIPTION_FAILED;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public Patient getPatient() {
        return patient;
    }

    public AppUser getDoctorUser() {
        return doctorUser;
    }

    public ConsultationStatus getStatus() {
        return status;
    }

    public Instant getPatientInformedAt() {
        return patientInformedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

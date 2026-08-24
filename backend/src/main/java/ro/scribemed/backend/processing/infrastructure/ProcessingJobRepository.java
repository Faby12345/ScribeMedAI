package ro.scribemed.backend.processing.infrastructure;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.domain.ProcessingJobStatus;
import ro.scribemed.backend.processing.domain.ProcessingJobType;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJob, UUID> {

    @Query(value = """
            SELECT *
            FROM processing_job
            WHERE job_type = 'TRANSCRIPTION'
              AND status IN ('PENDING', 'RETRY')
              AND next_attempt_at <= NOW()
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Optional<ProcessingJob> findNextTranscriptionJobForUpdate();


    @Query(value = """
            SELECT *
            FROM processing_job
            WHERE job_type = 'STRUCTURE_NOTES'
              AND status IN ('PENDING', 'RETRY')
              AND next_attempt_at <= NOW()
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Optional<ProcessingJob> findNextNoteJobForUpdate();

    @Query(value = """
            SELECT *
            FROM processing_job
            WHERE job_type = 'STRUCTURE_TRANSCRIPTION'
              AND status IN ('PENDING', 'RETRY')
              AND next_attempt_at <= NOW()
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Optional<ProcessingJob> findNextTranscriptStructureJobForUpdate();

    Optional<ProcessingJob> findByIdAndTenant_Id(UUID id, UUID tenantId);

    boolean existsByConsultation_IdAndTenant_IdAndJobTypeAndStatusIn(
            UUID consultationId,
            UUID tenantId,
            ProcessingJobType jobType,
            Collection<ProcessingJobStatus> statuses
    );
}

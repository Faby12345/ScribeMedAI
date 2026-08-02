package ro.scribemed.backend.processing.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ro.scribemed.backend.processing.domain.ProcessingJob;

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

    Optional<ProcessingJob> findByIdAndTenant_Id(UUID id, UUID tenantId);
}

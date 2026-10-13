package ro.scribemed.backend.mockPias.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ro.scribemed.backend.mockPias.domain.ProcessedJob;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedJobRepository extends JpaRepository<ProcessedJob, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_kafka_job (
                id,
                tenant_id,
                job_id,
                consumer_name,
                idempotency_key,
                processed_at
            )
            VALUES (
                :id,
                :tenantId,
                :jobId,
                :consumerName,
                :idempotencyKey,
                :processedAt
            )
            ON CONFLICT (tenant_id, consumer_name, idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("tenantId") UUID tenantId,
            @Param("jobId") UUID jobId,
            @Param("consumerName") String consumerName,
            @Param("idempotencyKey") String idempotencyKey,
            @Param("processedAt") Instant processedAt
    );

    Optional<ProcessedJob> findByTenant_IdAndConsumerNameAndIdempotencyKey(
            UUID tenantId,
            String consumerName,
            String idempotencyKey
    );
}

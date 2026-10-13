package ro.scribemed.backend.mockPias.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import ro.scribemed.backend.mockPias.domain.ProcessedJob;
import ro.scribemed.backend.tenancy.domain.Tenant;
import ro.scribemed.backend.tenancy.domain.TenantStatus;
import ro.scribemed.backend.tenancy.infrastructure.TenantRepository;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class ProcessedJobRepositoryIntegrationTest {

    private static final String CONSUMER_NAME = "pias-integration";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg18")
                    .asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("scribemed_processed_job_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private ProcessedJobRepository processedJobRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void claimsEachIdempotencyKeyOncePerTenantAndConsumer() {
        Tenant tenant = tenantRepository.saveAndFlush(
                new Tenant("Clinică A", TenantStatus.ACTIVE)
        );
        Tenant otherTenant = tenantRepository.saveAndFlush(
                new Tenant("Clinică B", TenantStatus.ACTIVE)
        );
        UUID jobId = UUID.randomUUID();
        String idempotencyKey = UUID.randomUUID().toString();
        Instant processedAt = Instant.parse("2026-10-10T10:00:00.123456Z");

        int firstClaim = claim(
                tenant.getId(),
                jobId,
                idempotencyKey,
                processedAt
        );
        int duplicateClaim = claim(
                tenant.getId(),
                UUID.randomUUID(),
                idempotencyKey,
                processedAt.plusSeconds(1)
        );
        int otherTenantClaim = claim(
                otherTenant.getId(),
                UUID.randomUUID(),
                idempotencyKey,
                processedAt.plusSeconds(2)
        );

        assertThat(firstClaim).isEqualTo(1);
        assertThat(duplicateClaim).isZero();
        assertThat(otherTenantClaim).isEqualTo(1);

        ProcessedJob processedJob = processedJobRepository
                .findByTenant_IdAndConsumerNameAndIdempotencyKey(
                        tenant.getId(),
                        CONSUMER_NAME,
                        idempotencyKey
                )
                .orElseThrow();

        assertThat(processedJob.getJobId()).isEqualTo(jobId);
        assertThat(processedJob.getProcessedAt()).isEqualTo(processedAt);
    }

    private int claim(
            UUID tenantId,
            UUID jobId,
            String idempotencyKey,
            Instant processedAt
    ) {
        return processedJobRepository.insertIfAbsent(
                UUID.randomUUID(),
                tenantId,
                jobId,
                CONSUMER_NAME,
                idempotencyKey,
                processedAt
        );
    }
}

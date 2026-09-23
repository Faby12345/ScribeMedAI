package ro.scribemed.backend.knowledge.infrastructure;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
class KnowledgeTenantMigrationIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg18")
                    .asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("scribemed_migration_test")
            .withUsername("test")
            .withPassword("test");

    @Test
    void backfillsOwnershipAndEnforcesTenantConsistency() throws SQLException {
        migrateTo("13");

        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        try (Connection connection = connection()) {
            insertTenant(connection, tenantId, "Clinică A");
            insertTenant(connection, otherTenantId, "Clinică B");
            insertDocument(connection, documentId);
            insertChunk(connection, chunkId, documentId);
            insertJob(connection, jobId, tenantId, documentId);
        }

        migrateTo("14");

        try (Connection connection = connection()) {
            assertEquals(
                    tenantId,
                    tenantIdFor(connection, "knowledge_document", documentId)
            );
            assertEquals(
                    tenantId,
                    tenantIdFor(connection, "knowledge_chunk", chunkId)
            );

            assertThrows(SQLException.class, () -> {
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE processing_job
                        SET tenant_id = ?
                        WHERE id = ?
                        """)) {
                    statement.setObject(1, otherTenantId);
                    statement.setObject(2, jobId);
                    statement.executeUpdate();
                }
            });
        }
    }

    private void migrateTo(String version) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target(MigrationVersion.fromVersion(version))
                .load()
                .migrate();
    }

    private Connection connection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );
    }

    private void insertTenant(
            Connection connection,
            UUID tenantId,
            String name
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tenant (id, name, status)
                VALUES (?, ?, 'ACTIVE')
                """)) {
            statement.setObject(1, tenantId);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }

    private void insertDocument(
            Connection connection,
            UUID documentId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO knowledge_document (
                    id, title, source_institution, status, original_filename,
                    checksum, object_key
                )
                VALUES (?, 'Ghid clinic', 'Ministerul Sănătății', 'ACTIVE',
                        'ghid.pdf', ?, ?)
                """)) {
            statement.setObject(1, documentId);
            statement.setString(2, "a".repeat(64));
            statement.setString(3, "tenant/test/knowledge/ghid.pdf");
            statement.executeUpdate();
        }
    }

    private void insertChunk(
            Connection connection,
            UUID chunkId,
            UUID documentId
    ) throws SQLException {
        String vector = IntStream.range(0, 384)
                .mapToObj(index -> "0")
                .collect(Collectors.joining(",", "[", "]"));
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO knowledge_chunk (
                    id, document_id, chunk_index, content, embedding
                )
                VALUES (?, ?, 0, 'Fragment test', CAST(? AS vector))
                """)) {
            statement.setObject(1, chunkId);
            statement.setObject(2, documentId);
            statement.setString(3, vector);
            statement.executeUpdate();
        }
    }

    private void insertJob(
            Connection connection,
            UUID jobId,
            UUID tenantId,
            UUID documentId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO processing_job (
                    id, tenant_id, knowledge_document_id, target_type,
                    job_type, status, attempt_count, max_attempts
                )
                VALUES (?, ?, ?, 'KNOWLEDGE_DOCUMENT', 'INGEST_DOCUMENT',
                        'SUCCEEDED', 1, 3)
                """)) {
            statement.setObject(1, jobId);
            statement.setObject(2, tenantId);
            statement.setObject(3, documentId);
            statement.executeUpdate();
        }
    }

    private UUID tenantIdFor(
            Connection connection,
            String table,
            UUID id
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT tenant_id FROM " + table + " WHERE id = ?"
        )) {
            statement.setObject(1, id);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getObject("tenant_id", UUID.class);
            }
        }
    }
}

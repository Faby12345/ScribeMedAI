package ro.scribemed.backend.knowledge.infrastructure;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;
import ro.scribemed.backend.audit.application.AuditService;
import ro.scribemed.backend.knowledge.application.pdf.PdfService;
import ro.scribemed.backend.knowledge.dto.StoredPdf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class LocalPdfStorageService extends PdfService {

    private final Path storageRoot;
    private final AuditService auditService;

    public LocalPdfStorageService(
            @Value("${scribemed.pdf.local-storage-dir}") Path storageRoot,
            AuditService auditService) {
        this.storageRoot = storageRoot
                .toAbsolutePath()
                .normalize();
        this.auditService = auditService;
    }

    @Override
    public StoredPdf store(
            UUID tenantId,
            InputStream inputStream,
            long sizeBytes) throws IOException{

        String objectKey = buildObjectKey(tenantId);

        Path destination = resolveObjectKey(objectKey);
        Files.createDirectories(destination.getParent());

        MessageDigest digest = sha256();
        try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
            Files.copy(digestInputStream, destination);
        }

        return new StoredPdf(
                objectKey,
                HexFormat.of().formatHex(digest.digest()),
                sizeBytes
        );

    }

    @Override
    public byte[] read(String objectKey) throws IOException {
        return Files.readAllBytes(resolveObjectKey(objectKey));
    }

    private Path resolveObjectKey(String objectKey) {
        Path resolved = storageRoot.resolve(objectKey).normalize();

        if (!resolved.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid PDF object key");
        }

        return resolved;
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }



}

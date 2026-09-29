package ro.scribemed.backend.knowledge.dto;

public record StoredPdf(
        String objectKey,
        String checkSumSha256,
        long sizeBytes
) {
}

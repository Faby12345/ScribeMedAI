package ro.scribemed.backend.audio.application;

public record StoredAudio(
        String objectKey,
        String checksumSha256,
        long sizeBytes
) {
}

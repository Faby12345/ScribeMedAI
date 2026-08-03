package ro.scribemed.backend.audio.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ro.scribemed.backend.audio.application.AudioStorageService;
import ro.scribemed.backend.audio.application.StoredAudio;

@Service
public class LocalAudioStorageService implements AudioStorageService {

    private final Path storageRoot;

    public LocalAudioStorageService(
            @Value("${scribemed.audio.local-storage-dir}") String localStorageDir
    ) {
        this.storageRoot = Path.of(localStorageDir).toAbsolutePath().normalize();
    }

    @Override
    public StoredAudio store(String objectKey, InputStream inputStream, long sizeBytes) throws IOException {
        Path destination = resolveObjectKey(objectKey);
        Files.createDirectories(destination.getParent());

        MessageDigest digest = sha256();
        try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
            Files.copy(digestInputStream, destination);
        }

        return new StoredAudio(
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
            throw new IllegalArgumentException("Invalid audio object key");
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

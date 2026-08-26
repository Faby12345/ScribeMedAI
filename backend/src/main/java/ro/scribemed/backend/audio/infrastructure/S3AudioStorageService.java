package ro.scribemed.backend.audio.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ro.scribemed.backend.audio.application.AudioStorageService;
import ro.scribemed.backend.audio.application.StoredAudio;
import ro.scribemed.backend.audio.config.S3AudioStorageProperties;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@ConditionalOnProperty(prefix = "scribemed.audio.storage", name = "backend", havingValue = "s3")
public class S3AudioStorageService implements AudioStorageService {

    private final S3Client s3Client;
    private final S3AudioStorageProperties properties;

    public S3AudioStorageService(S3Client s3Client, S3AudioStorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    @Override
    public StoredAudio store(String objectKey, InputStream inputStream, long sizeBytes) throws IOException {
        validateObjectKey(objectKey);
        String bucket = requiredBucket();
        MessageDigest digest = sha256();

        try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build(),
                    RequestBody.fromInputStream(digestInputStream, sizeBytes)
            );
        } catch (S3Exception error) {
            throw new IOException("Audio object could not be stored in S3", error);
        }

        return new StoredAudio(
                objectKey,
                HexFormat.of().formatHex(digest.digest()),
                sizeBytes
        );
    }

    @Override
    public byte[] read(String objectKey) throws IOException {
        validateObjectKey(objectKey);

        try {
            ResponseBytes<GetObjectResponse> object = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(requiredBucket())
                    .key(objectKey)
                    .build());
            return object.asByteArray();
        } catch (S3Exception error) {
            throw new IOException("Audio object could not be read from S3", error);
        }
    }

    private String requiredBucket() {
        String bucket = properties.getBucket();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("S3 audio bucket is not configured");
        }
        return bucket;
    }

    private void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || objectKey.startsWith("/") || objectKey.contains("..")) {
            throw new IllegalArgumentException("Invalid audio object key");
        }
    }

    private MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }
}

package ro.scribemed.backend.audio.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ro.scribemed.backend.audio.application.StoredAudio;
import ro.scribemed.backend.audio.config.S3AudioStorageProperties;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;

class S3AudioStorageServiceTests {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3AudioStorageProperties properties = new S3AudioStorageProperties();
    private final S3AudioStorageService service = new S3AudioStorageService(s3Client, properties);

    S3AudioStorageServiceTests() {
        properties.setBucket("audio-bucket");
    }

    @Test
    void storeUploadsObjectAndReturnsChecksum() throws Exception {
        byte[] content = "audio".getBytes(StandardCharsets.UTF_8);
        doAnswer(invocation -> {
            RequestBody body = invocation.getArgument(1);
            try (InputStream inputStream = body.contentStreamProvider().newStream()) {
                inputStream.transferTo(OutputStream.nullOutputStream());
            }
            return null;
        }).when(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));

        StoredAudio storedAudio = service.store(
                "tenant/tenant-id/consultation/consultation-id/audio.webm",
                new ByteArrayInputStream(content),
                content.length
        );

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest request = requestCaptor.getValue();

        assertThat(request.bucket()).isEqualTo("audio-bucket");
        assertThat(request.key()).isEqualTo("tenant/tenant-id/consultation/consultation-id/audio.webm");
        assertThat(storedAudio.checksumSha256())
                .isEqualTo("6ed8919ce20490a5e3ad8630a4fab69475297abd07db73918dd5f36fcfaeb11b");
        assertThat(storedAudio.sizeBytes()).isEqualTo(content.length);
    }

    @Test
    void readDownloadsObjectBytes() throws Exception {
        byte[] content = "audio".getBytes(StandardCharsets.UTF_8);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), content));

        byte[] result = service.read("tenant/tenant-id/consultation/consultation-id/audio.webm");

        assertThat(result).isEqualTo(content);
    }

    @Test
    void rejectsInvalidObjectKey() {
        assertThatThrownBy(() -> service.read("../audio.webm"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid audio object key");
    }
}

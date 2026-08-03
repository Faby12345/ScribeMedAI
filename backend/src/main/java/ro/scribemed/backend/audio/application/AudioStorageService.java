package ro.scribemed.backend.audio.application;

import java.io.IOException;
import java.io.InputStream;

public interface AudioStorageService {

    StoredAudio store(
            String objectKey,
            InputStream inputStream,
            long sizeBytes
    ) throws IOException;

    byte[] read(String objectKey) throws IOException;
}

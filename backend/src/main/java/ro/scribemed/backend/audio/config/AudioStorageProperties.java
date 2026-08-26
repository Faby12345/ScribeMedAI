package ro.scribemed.backend.audio.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scribemed.audio.storage")
public class AudioStorageProperties {

    private Backend backend = Backend.LOCAL;

    public Backend getBackend() {
        return backend;
    }

    public void setBackend(Backend backend) {
        this.backend = backend;
    }

    public enum Backend {
        LOCAL,
        S3
    }
}

package ro.scribemed.backend.transcription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scribemed.transcription.deepgram")
public class DeepgramTranscriptionProperties {

    private String apiKey;
    private String baseUrl = "https://api.deepgram.com";
    private String model = "nova-3";
    private String language = "ro";
    private boolean smartFormat = true;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isSmartFormat() {
        return smartFormat;
    }

    public void setSmartFormat(boolean smartFormat) {
        this.smartFormat = smartFormat;
    }
}

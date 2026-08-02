package ro.scribemed.backend.transcription.application;

public interface TranscriptionProvider {

    TranscriptionResult transcribe(TranscriptionRequest request);
}

package ro.scribemed.backend.transcription.application;

import ro.scribemed.backend.transcription.dto.TranscriptionRequest;

public interface TranscriptionProvider {

    TranscriptionResult transcribe(TranscriptionRequest request);
}

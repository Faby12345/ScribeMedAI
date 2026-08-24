package ro.scribemed.backend.consultation.application;

import ro.scribemed.backend.consultation.dto.NotesRequest;
import ro.scribemed.backend.consultation.dto.TranscriptNoteGenerationRequest;

public interface ClinicalNoteGenerationProvider {

    boolean isAvailable();

    ClinicalNoteGenerationResult generateFromDoctorNotes(NotesRequest rawNotes);

    ClinicalNoteGenerationResult generateFromTranscript(TranscriptNoteGenerationRequest transcript);

}

package ro.scribemed.backend.consultation.application;

public interface ClinicalNoteGenerationProvider {

    boolean isAvailable();

    ClinicalNoteGenerationResult generate(NotesRequest rawNotes);

}

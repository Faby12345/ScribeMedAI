package ro.scribemed.backend.knowledge.application;

import org.junit.jupiter.api.Test;
import ro.scribemed.backend.consultation.domain.Consultation;
import ro.scribemed.backend.knowledge.domain.KnowledgeDocument;
import ro.scribemed.backend.processing.domain.ProcessingJob;
import ro.scribemed.backend.processing.domain.ProcessingJobTargetType;
import ro.scribemed.backend.processing.domain.ProcessingJobType;
import ro.scribemed.backend.tenancy.domain.Tenant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ProcessingJobTargetTest {

    @Test
    void createsAConsultationJobWithTheMatchingTarget() {
        Consultation consultation = mock(Consultation.class);

        ProcessingJob job = new ProcessingJob(
                mock(Tenant.class),
                consultation,
                ProcessingJobType.TRANSCRIPTION
        );

        assertEquals(ProcessingJobTargetType.CONSULTATION, job.getTargetType());
        assertEquals(consultation, job.getConsultation());
        assertNull(job.getKnowledgeDocument());
    }

    @Test
    void createsAKnowledgeDocumentJobWithTheMatchingTarget() {
        KnowledgeDocument document = mock(KnowledgeDocument.class);

        ProcessingJob job = new ProcessingJob(
                mock(Tenant.class),
                document,
                ProcessingJobType.INGEST_DOCUMENT
        );

        assertEquals(
                ProcessingJobTargetType.KNOWLEDGE_DOCUMENT,
                job.getTargetType()
        );
        assertEquals(document, job.getKnowledgeDocument());
        assertNull(job.getConsultation());
    }

    @Test
    void rejectsAJobTypeThatDoesNotMatchTheTarget() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProcessingJob(
                        mock(Tenant.class),
                        mock(KnowledgeDocument.class),
                        ProcessingJobType.TRANSCRIPTION
                )
        );
    }
}

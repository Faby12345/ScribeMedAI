package ro.scribemed.backend.document.dto;

import java.util.UUID;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ApproveDocumentRequest(
        @NotNull(message = "Versiunea documentului este obligatorie.")
        UUID versionId,

        @AssertTrue(message = "Confirmarea revizuirii medicale este obligatorie.")
        boolean doctorReviewedAndApproved
) {
}

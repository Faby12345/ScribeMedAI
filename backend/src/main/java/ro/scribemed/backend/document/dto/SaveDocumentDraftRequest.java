package ro.scribemed.backend.document.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveDocumentDraftRequest(
        @Valid
        @NotNull(message = "Draftul SOAP este obligatoriu.")
        SoapDraftRequest draft,

        List<@Size(max = 200, message = "Marcajul de revizuire nu poate depasi 200 de caractere.") String> reviewFlags
) {

    public record SoapDraftRequest(
            @Size(max = 20000, message = "Sectiunea subiectiva nu poate depasi 20000 de caractere.")
            String subjective,

            @Size(max = 20000, message = "Sectiunea obiectiva nu poate depasi 20000 de caractere.")
            String objective,

            @Size(max = 20000, message = "Sectiunea evaluare nu poate depasi 20000 de caractere.")
            String assessment,

            @Size(max = 20000, message = "Sectiunea plan nu poate depasi 20000 de caractere.")
            String plan
    ) {
    }
}

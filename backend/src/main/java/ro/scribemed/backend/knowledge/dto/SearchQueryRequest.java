package ro.scribemed.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SearchQueryRequest(

        @NotBlank(message = "The query can't be bank")
        @Size(max = 2000, message = "The query can't be greater than 2000 words")
        String query,


        /// Empty or null list of documents means all the doc for that tenant
        @Size(max = 20, message = "The number of files can't can't be grater than 20")
        List<UUID> documentsIds
) {
}

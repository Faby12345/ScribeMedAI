package ro.scribemed.backend.knowledge.dto;

import java.util.List;
import java.util.UUID;

public record SearchQueryRequest(
        String query,
        List<UUID> documentsIds
) {
}

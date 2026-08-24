package ro.scribemed.backend.consultation.infrastructure.dto;

import java.util.List;

public record HuggingFaceChatRequest(
        String model,
        List<Message> messages,
        Double temperature,
        Integer max_tokens
) {

    public record Message(
            String role,
            String content
    ) {}
}
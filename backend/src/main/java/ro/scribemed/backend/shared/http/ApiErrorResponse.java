package ro.scribemed.backend.shared.http;

public record ApiErrorResponse(
        String code,
        String message
) {
}

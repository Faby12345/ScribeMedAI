package ro.scribemed.backend.mockPias.infrastructure.messaging;

public final class PiasTopics {

    public static final String VALIDATION_REQUESTED =
            "scribemed.pias.validation.requested.v1";

    public static final String VALIDATION_DEAD_LETTER =
            "scribemed.pias.validation.requested.v1-dlt";

    public static final String VALIDATION_CONSUMER_GROUP = "pias-integration";

    public PiasTopics(){}
}

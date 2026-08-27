package ro.scribemed.backend.shared.rateLimit.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rate_limit_window")
public class RateLimitWindow {

    protected RateLimitWindow() {}

    public RateLimitWindow(RateLimitScope scope, String identifier, Instant updatedAt) {
        if(scope == null){
            throw new RuntimeException("scope cannot be null");
        }

        this.scope = scope;
        this.identifier = identifier;
        this.windowStartedAt = updatedAt;
        this.requestCount = 1;
        this.updatedAt = updatedAt;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private RateLimitScope scope;

    @NotNull
    private String identifier;

    @NotNull
    private Instant windowStartedAt;


    private int requestCount;

    @NotNull
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void increment(Instant now){
        this.requestCount++;
        this.updatedAt = now;
    }

    public void reset(Instant now){
        this.requestCount = 1;
        this.updatedAt = now;
        this.windowStartedAt = now;
    }
}

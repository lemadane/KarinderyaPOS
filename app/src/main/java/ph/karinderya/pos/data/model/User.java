package ph.karinderya.pos.data.model;

import java.time.Instant;
import java.util.UUID;

public record User(
    UUID id,
    String username,
    String fullName,
    Instant createdAt) {
}

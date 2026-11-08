package ph.karinderya.pos.data.model;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomDiscount(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name,
        @JsonProperty("percent") int percent) {

    public static final List<Integer> ALLOWED_PERCENTAGES = List.of(5, 8, 10, 12, 15, 18, 20);

    @JsonCreator
    public CustomDiscount {
        if (id == null) {
            throw new IllegalArgumentException("Discount ID is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Discount name is required");
        }
        name = name.trim();
        if (!ALLOWED_PERCENTAGES.contains(percent)) {
            throw new IllegalArgumentException("Discount must be 5%, 8%, 10%, 12%, 15%, 18%, or 20%");
        }
    }
}

package ph.karinderya.pos.data.model;

public record TopItem(
    String name,
    int quantity,
    long totalCents) {
}

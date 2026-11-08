package ph.karinderya.pos.data.model;

public enum OrderStatus {
  PREPARING("Preparing"),
  READY("Ready"),
  SERVED("Served"),
  CANCELLED("Cancelled");

  final String label;

  OrderStatus(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  /** Next status in the kitchen flow, or {@code null} when the order is already closed. */
  public OrderStatus next() {
    return switch (this) {
      case PREPARING -> READY;
      case READY -> SERVED;
      case SERVED, CANCELLED -> null;
    };
  }

  public boolean isActive() {
    return this == PREPARING || this == READY;
  }
}

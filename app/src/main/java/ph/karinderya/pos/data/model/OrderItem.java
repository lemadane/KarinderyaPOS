package ph.karinderya.pos.data.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A line of a saved order. Name and price are snapshots so history survives menu edits. */
public record OrderItem(
    @JsonProperty("id")
    UUID id,
    @JsonProperty("orderId")
    UUID orderId,
    @JsonProperty("menuItemId")
    UUID menuItemId,
    @JsonProperty("name")
    String name,
    @JsonProperty("unitPriceCents")
    long unitPriceCents,
    @JsonProperty("quantity")
    int quantity,
    @JsonProperty("lineTotalCents")
    long lineTotalCents) {

  @JsonCreator
  public OrderItem {
  }
}

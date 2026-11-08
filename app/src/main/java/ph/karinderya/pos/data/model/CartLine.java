package ph.karinderya.pos.data.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CartLine(
    @JsonProperty("menuItemId")
    UUID menuItemId,
    @JsonProperty("name")
    String name,
    @JsonProperty("unitPriceCents")
    long unitPriceCents,
    @JsonProperty("quantity")
    int quantity,
    @JsonProperty("photoPath")
    String photoPath) {

  @JsonCreator
  public CartLine {
  }

  public static CartLine of(final MenuItem item) {
    return new CartLine(
        item.id(),
        item.name(),
        item.priceCents(),
        1,
        item.photoPath());
  }

  public CartLine withQuantity(final int newQuantity) {
    return new CartLine(
        menuItemId,
        name,
        unitPriceCents,
        newQuantity,
        photoPath);
  }

  public long lineTotalCents() {
    return unitPriceCents * quantity;
  }
}

package ph.karinderya.pos.data.model;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BackupData(
    @JsonProperty("version")
    int version,
    @JsonProperty("exportedAt")
    Instant exportedAt,
    @JsonProperty("menuItems")
    List<BackupMenuItem> menuItems,
    @JsonProperty("orders")
    List<Order> orders,
    @JsonProperty("discounts")
    List<CustomDiscount> discounts) {

  public static final int CURRENT_VERSION = 2;

  @JsonCreator
  public BackupData {
    menuItems = menuItems == null
        ? List.of()
        : List.copyOf(menuItems);
    orders = orders == null
        ? List.of()
        : List.copyOf(orders);
    discounts = discounts == null ? List.of() : List.copyOf(discounts);
  }
}

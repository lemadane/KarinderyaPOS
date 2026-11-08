package ph.karinderya.pos.data.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record Order(
    @JsonProperty("id")
    UUID id,
    @JsonProperty("orderNo")
    String orderNo,
    @JsonProperty("orderType")
    OrderType orderType,
    @JsonProperty("customerLabel")
    String customerLabel,
    @JsonProperty("status")
    OrderStatus status,
    @JsonProperty("paymentMethod")
    PaymentMethod paymentMethod,
    @JsonProperty("discountType")
    DiscountType discountType,
    @JsonProperty("subtotalCents")
    long subtotalCents,
    @JsonProperty("discountCents")
    long discountCents,
    @JsonProperty("totalCents")
    long totalCents,
    @JsonProperty("cashTenderedCents")
    long cashTenderedCents,
    @JsonProperty("changeCents")
    long changeCents,
    @JsonProperty("notes")
    String notes,
    @JsonProperty("createdByUserId")
    UUID createdByUserId,
    @JsonProperty("createdByName")
    String createdByName,
    @JsonProperty("createdAt")
    Instant createdAt,
    @JsonProperty("updatedAt")
    Instant updatedAt,
    @JsonProperty("items")
    List<OrderItem> items,
    @JsonProperty("discountName")
    String discountName,
    @JsonProperty("discountPercent")
    int discountPercent) {

  @JsonCreator
  public Order {
    if (discountType == DiscountType.CUSTOM) {
      if (discountName == null || discountName.isBlank()
          || !CustomDiscount.ALLOWED_PERCENTAGES.contains(discountPercent)) {
        throw new IllegalArgumentException("Custom discount name and valid percentage are required");
      }
    } else {
      discountName = discountType.label();
      discountPercent = discountType.percent();
    }
    items = items == null
        ? List.of()
        : List.copyOf(items);
  }

  public Order withItems(List<OrderItem> newItems) {
    return new Order(
        id,
        orderNo,
        orderType,
        customerLabel,
        status,
        paymentMethod,
        discountType,
        subtotalCents,
        discountCents,
        totalCents,
        cashTenderedCents,
        changeCents,
        notes,
        createdByUserId,
        createdByName,
        createdAt,
        updatedAt,
        newItems,
        discountName,
        discountPercent);
  }

  public int itemCount() {
    return items.stream()
        .mapToInt(OrderItem::quantity)
        .sum();
  }
}

package ph.karinderya.pos.data.model;

import java.util.List;

public record OrderDraft(
    OrderType orderType,
    String customerLabel,
    DiscountType discountType,
    PaymentMethod paymentMethod,
    long cashTenderedCents,
    String notes,
    List<CartLine> lines,
    CustomDiscount customDiscount) {

  public OrderDraft {
    lines = List.copyOf(lines);
    if ((discountType == DiscountType.CUSTOM) != (customDiscount != null)) {
      throw new IllegalArgumentException("Custom discount selection does not match discount type");
    }
  }

  public int discountPercent() {
    return customDiscount == null ? discountType.percent() : customDiscount.percent();
  }

  public String discountName() {
    return customDiscount == null ? discountType.label() : customDiscount.name();
  }
}

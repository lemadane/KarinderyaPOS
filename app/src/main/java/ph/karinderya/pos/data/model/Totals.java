package ph.karinderya.pos.data.model;

import java.util.List;

public record Totals(
    long subtotalCents,
    long discountCents,
    long totalCents) {

  public static Totals of(
      List<CartLine> lines,
      DiscountType discountType) {
    if (discountType == DiscountType.CUSTOM) {
      throw new IllegalArgumentException("Custom discount percentage is required");
    }
    return of(lines, discountType.percent());
  }

  public static Totals of(List<CartLine> lines, int percent) {
    if (percent != 0 && !CustomDiscount.ALLOWED_PERCENTAGES.contains(percent)) {
      throw new IllegalArgumentException("Discount must be 0%, 5%, 8%, 10%, 12%, 15%, 18%, or 20%");
    }
    long subtotal =
        lines.stream()
        .mapToLong(CartLine::lineTotalCents)
        .sum();
    long discount =
        (subtotal * percent + 50) / 100;
    return new Totals(
        subtotal,
        discount,
        subtotal - discount);
  }
}

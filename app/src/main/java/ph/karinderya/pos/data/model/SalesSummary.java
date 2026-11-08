package ph.karinderya.pos.data.model;

import java.util.List;
import java.util.Map;

public record SalesSummary(
    int orderCount,
    int cancelledCount,
    long grossCents,
    long discountCents,
    long netCents,
    Map<PaymentMethod, Long> byPayment,
    List<TopItem> topItems) {

  public SalesSummary {
    byPayment = Map.copyOf(byPayment);
    topItems = List.copyOf(topItems);
  }
}

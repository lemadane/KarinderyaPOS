package ph.karinderya.pos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ph.karinderya.pos.data.model.DiscountType;
import ph.karinderya.pos.data.model.Order;
import ph.karinderya.pos.data.model.OrderItem;
import ph.karinderya.pos.data.model.OrderStatus;
import ph.karinderya.pos.data.model.OrderType;
import ph.karinderya.pos.data.model.PaymentMethod;

final class TestData {

  static final Instant CREATED =
      Instant.parse("2026-10-19T04:30:00Z");

  TestData() {
  }

  static Order order() {
    UUID orderId =
        UUID.fromString("7b0c1b6e-3f4f-4a51-9d0d-2b1f0c8a9e11");
    List<OrderItem> items = List.of(
        new OrderItem(UUID.randomUUID(), orderId, UUID.randomUUID(), "Adobong Manok", 8000, 2, 16000),
        new OrderItem(
            UUID.randomUUID(),
            orderId,
            null,
            "Kanin",
            1500,
            2,
            3000));
    return new Order(orderId, "261019-001", OrderType.DINE_IN, "Table 3", OrderStatus.PREPARING,
        PaymentMethod.CASH, DiscountType.SENIOR, 19000, 3800, 15200, 20000, 4800, "Extra sabaw",
        UUID.randomUUID(
        ),
        "Juan Dela Cruz",
        CREATED,
        CREATED,
        items, null, 0);
  }
}

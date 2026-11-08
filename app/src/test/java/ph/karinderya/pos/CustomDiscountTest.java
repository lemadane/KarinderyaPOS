package ph.karinderya.pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

import ph.karinderya.pos.data.model.BackupData;
import ph.karinderya.pos.data.model.CartLine;
import ph.karinderya.pos.data.model.CustomDiscount;
import ph.karinderya.pos.data.model.DiscountType;
import ph.karinderya.pos.data.model.Order;
import ph.karinderya.pos.data.model.OrderDraft;
import ph.karinderya.pos.data.model.OrderType;
import ph.karinderya.pos.data.model.PaymentMethod;
import ph.karinderya.pos.data.model.Totals;
import ph.karinderya.pos.util.Json;
import ph.karinderya.pos.util.ReceiptFormatter;

public class CustomDiscountTest {

    final List<CartLine> lines = List.of(new CartLine(UUID.randomUUID(), "Adobo", 1999, 1, null));

    @Test
    public void calculatesEveryAllowedRateWithCentRounding() {
        int[] rates = {5, 8, 10, 12, 15, 18, 20};
        long[] discounts = {100, 160, 200, 240, 300, 360, 400};
        assertEquals(List.of(5, 8, 10, 12, 15, 18, 20), CustomDiscount.ALLOWED_PERCENTAGES);
        for (int i = 0; i < rates.length; i++) {
            CustomDiscount discount = new CustomDiscount(UUID.randomUUID(), "Discount", rates[i]);
            assertEquals(rates[i], discount.percent());
            Totals totals = Totals.of(lines, rates[i]);
            assertEquals(1999, totals.subtotalCents());
            assertEquals(discounts[i], totals.discountCents());
            assertEquals(1999 - discounts[i], totals.totalCents());
        }
        assertEquals(1, Totals.of(List.of(new CartLine(UUID.randomUUID(), "Rice", 10, 1, null)), 5)
                .discountCents());
    }

    @Test
    public void validatesNameIdentityAndAllowedRates() {
        UUID id = UUID.randomUUID();
        assertEquals("Friend Discount (5%)", new CustomDiscount(id, " Friend Discount (5%) ", 5).name());
        assertThrows(IllegalArgumentException.class, () -> new CustomDiscount(id, " ", 5));
        assertThrows(IllegalArgumentException.class, () -> new CustomDiscount(id, null, 5));
        assertThrows(IllegalArgumentException.class, () -> new CustomDiscount(null, "Friend", 5));
        for (int rate : new int[]{-1, 0, 1, 6, 9, 11, 14, 16, 19, 21, 100}) {
            assertThrows(IllegalArgumentException.class, () -> new CustomDiscount(id, "Friend", rate));
        }
        assertThrows(IllegalArgumentException.class, () -> Totals.of(lines, 16));
        assertThrows(IllegalArgumentException.class, () -> Totals.of(lines, DiscountType.CUSTOM));
    }

    @Test
    public void draftSelectsExactlyOneDiscount() {
        CustomDiscount discount = new CustomDiscount(UUID.randomUUID(), "Friend Discount (5%)", 5);
        OrderDraft draft = draft(DiscountType.CUSTOM, discount);
        assertEquals(5, draft.discountPercent());
        assertEquals("Friend Discount (5%)", draft.discountName());
        assertEquals(100, Totals.of(draft.lines(), draft.discountPercent()).discountCents());
        assertEquals(20, draft(DiscountType.SENIOR, null).discountPercent());
        assertEquals(20, draft(DiscountType.PWD, null).discountPercent());
        assertEquals(0, draft(DiscountType.NONE, null).discountPercent());
        assertThrows(IllegalArgumentException.class, () -> draft(DiscountType.SENIOR, discount));
        assertThrows(IllegalArgumentException.class, () -> draft(DiscountType.PWD, discount));
        assertThrows(IllegalArgumentException.class, () -> draft(DiscountType.NONE, discount));
        assertThrows(IllegalArgumentException.class, () -> draft(DiscountType.CUSTOM, null));
    }

    @Test
    public void backupAndReceiptRetainCustomOrderSnapshotAfterDefinitionChanges() throws Exception {
        CustomDiscount discount = new CustomDiscount(UUID.randomUUID(), "Friend Discount (5%)", 5);
        Order base = TestData.order();
        Order order = new Order(base.id(), base.orderNo(), base.orderType(), base.customerLabel(), base.status(),
                base.paymentMethod(), DiscountType.CUSTOM, 19000, 950, 18050, 20000, 1950, base.notes(),
                base.createdByUserId(), base.createdByName(), base.createdAt(), base.updatedAt(), base.items(),
                discount.name(), discount.percent());
        CustomDiscount edited = new CustomDiscount(discount.id(), "Special Discount (10%)", 10);
        BackupData backup = new BackupData(BackupData.CURRENT_VERSION, TestData.CREATED,
                List.of(), List.of(order), List.of(edited));
        BackupData restored = Json.mapper().readValue(Json.mapper().writeValueAsString(backup), BackupData.class);
        assertEquals(backup, restored);
        Order historical = restored.orders().get(0).withItems(base.items());
        assertEquals("Friend Discount (5%)", historical.discountName());
        assertEquals(5, historical.discountPercent());
        String receipt = ReceiptFormatter.format(historical, "KarinderyaPOS");
        assertTrue(receipt, receipt.contains("Friend Discount (5%)"));
        assertTrue(receipt, receipt.contains("-\u20B19.50"));
        assertTrue(receipt, receipt.contains("\u20B1180.50"));
        BackupData withoutDefinition = new BackupData(BackupData.CURRENT_VERSION, TestData.CREATED,
                List.of(), List.of(historical), List.of());
        assertEquals(historical, Json.mapper().readValue(Json.mapper().writeValueAsString(withoutDefinition),
                BackupData.class).orders().get(0));
    }

    @Test
    public void importsLegacyBackupWithNoDiscountDefinitionsOrOrderSnapshots() throws Exception {
        ObjectNode object =
                Json.mapper().readValue(Json.mapper().writeValueAsString(TestData.order()),
                        ObjectNode.class);
        object.remove(List.of("discountName", "discountPercent"));
        String legacyBackup = "{\"version\":1,\"exportedAt\":\"2026-10-19T04:30:00Z\","
                + "\"menuItems\":[],\"orders\":[" + object + "]}";
        BackupData restored = Json.mapper().readValue(legacyBackup, BackupData.class);
        assertTrue(restored.discounts().isEmpty());
        assertEquals("Senior Citizen", restored.orders().get(0).discountName());
        assertEquals(20, restored.orders().get(0).discountPercent());
        assertTrue(ReceiptFormatter.format(restored.orders().get(0), "Store").contains("Senior Citizen 20%"));
    }

    OrderDraft draft(DiscountType type, CustomDiscount discount) {
        return new OrderDraft(OrderType.DINE_IN, "", type, PaymentMethod.CASH, 2000, "", lines, discount);
    }
}

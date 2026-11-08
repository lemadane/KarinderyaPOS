package ph.karinderya.pos;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;
import java.util.UUID;

import ph.karinderya.pos.data.model.CartLine;
import ph.karinderya.pos.data.model.DiscountType;
import ph.karinderya.pos.data.model.Totals;

public class TotalsTest {

     final List<CartLine> lines = List.of(
            new CartLine(UUID.randomUUID(), "Adobo", 8000, 2, null),
            new CartLine(UUID.randomUUID(), "Kanin", 1500, 3, null));

    @Test
    public void noDiscount() {
        Totals totals = Totals.of(lines, DiscountType.NONE);
        assertEquals(20500, totals.subtotalCents());
        assertEquals(0, totals.discountCents());
        assertEquals(20500, totals.totalCents());
    }

    @Test
    public void seniorDiscountIsTwentyPercentRounded() {
        Totals totals = Totals.of(List.of(new CartLine(UUID.randomUUID(), "Turon", 1999, 1, null)),
                DiscountType.SENIOR);
        assertEquals(400, totals.discountCents());
        assertEquals(1599, totals.totalCents());
    }

    @Test
    public void emptyCart() {
        assertEquals(0, Totals.of(List.of(), DiscountType.PWD).totalCents());
    }
}

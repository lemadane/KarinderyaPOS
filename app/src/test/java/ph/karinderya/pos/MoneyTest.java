package ph.karinderya.pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import ph.karinderya.pos.util.Money;

public class MoneyTest {

    @Test
    public void formatsPesos() {
        assertEquals("\u20B10.00", Money.format(0));
        assertEquals("\u20B180.50", Money.format(8050));
        assertEquals("\u20B11,234,567.89", Money.format(123456789));
        assertEquals("-\u20B15.00", Money.format(-500));
    }

    @Test
    public void parsesUserInput() {
        assertEquals(8000, Money.parse("80"));
        assertEquals(125050, Money.parse("1,250.5"));
        assertEquals(1999, Money.parse("\u20B1 19.99"));
        assertEquals(1000, Money.parse("9.999"));
    }

    @Test
    public void rejectsInvalidInput() {
        assertThrows(NumberFormatException.class, () -> Money.parse(""));
        assertThrows(NumberFormatException.class, () -> Money.parse("abc"));
        assertThrows(NumberFormatException.class, () -> Money.parse("-1"));
        assertEquals(0, Money.parseOrZero("oops"));
    }

    @Test
    public void plainRoundTrips() {
        assertEquals(8050, Money.parse(Money.toPlain(8050)));
    }
}

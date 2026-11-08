package ph.karinderya.pos;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ph.karinderya.pos.util.ReceiptFormatter;

public class ReceiptFormatterTest {

    @Test
    public void containsTotalsAndFitsWidth() {
        String receipt = ReceiptFormatter.format(TestData.order(), "Aling Nena's Karinderya");
        assertTrue(receipt, receipt.contains("261019-001"));
        assertTrue(receipt, receipt.contains("Senior Citizen 20%"));
        assertTrue(receipt, receipt.contains("\u20B1152.00"));
        assertTrue(receipt, receipt.contains("\u20B148.00"));
        for (String line : receipt.split("\n")) {
            assertTrue("Too wide: " + line, line.length() <= ReceiptFormatter.WIDTH);
        }
    }
}

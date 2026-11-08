package ph.karinderya.pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ph.karinderya.pos.data.model.OrderStatus;

public class OrderStatusTest {

    @Test
    public void kitchenFlow() {
        assertEquals(OrderStatus.READY, OrderStatus.PREPARING.next());
        assertEquals(OrderStatus.SERVED, OrderStatus.READY.next());
        assertNull(OrderStatus.SERVED.next());
        assertNull(OrderStatus.CANCELLED.next());
        assertTrue(OrderStatus.READY.isActive());
        assertFalse(OrderStatus.CANCELLED.isActive());
    }
}

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderStatusTest {

    @Test
    void nextFollowsTheFullFlow() {
        assertEquals(OrderStatus.PREPARING, OrderStatus.RECEIVED.next());
        assertEquals(OrderStatus.READY, OrderStatus.PREPARING.next());
        assertEquals(OrderStatus.COMPLETED, OrderStatus.READY.next());
    }

    @Test
    void completedHasNoNextStatus() {
        assertThrows(IllegalStateException.class, () -> OrderStatus.COMPLETED.next());
    }

    @Test
    void canMoveForwardOneStepOnly() {
        assertTrue(OrderStatus.RECEIVED.canMoveTo(OrderStatus.PREPARING));
        assertFalse(OrderStatus.RECEIVED.canMoveTo(OrderStatus.READY));      // skipping
        assertFalse(OrderStatus.READY.canMoveTo(OrderStatus.PREPARING));     // going back
    }

    @Test
    void nullTargetIsRejected() {
        assertFalse(OrderStatus.RECEIVED.canMoveTo(null));
    }

    @Test
    void labelsAreReadable() {
        assertEquals("Ready for pickup", OrderStatus.READY.getLabel());
    }
}

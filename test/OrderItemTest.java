import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderItemTest {
    private MenuItem fries() { return new MenuItem(5, "Fries", MenuCategory.SIDES, 60); }

    @Test
    void subtotalIsPriceTimesQuantity() {
        assertEquals(180, new OrderItem(fries(), 3).getSubtotal());
    }

    @Test
    void badQuantityIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(fries(), 0));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(fries(), -2));
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(fries(), 21));
    }

    @Test
    void soldOutItemCannotBeOrdered() {
        MenuItem m = fries();
        m.setAvailable(false);
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(m, 1));
    }

    @Test
    void nullItemIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new OrderItem(null, 1));
    }

    @Test
    void copyKeepsItemAndQuantityButIsIndependent() {
        OrderItem original = new OrderItem(fries(), 3);
        OrderItem copy = new OrderItem(original);
        copy.setQuantity(7);
        assertEquals(3, original.getQuantity());
        assertEquals(7, copy.getQuantity());
        assertThrows(IllegalArgumentException.class, () -> new OrderItem((OrderItem) null));
    }
}

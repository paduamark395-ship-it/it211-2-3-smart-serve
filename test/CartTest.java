import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CartTest {
    private final MenuItem burger = new MenuItem(1, "Burger", MenuCategory.BURGERS, 120);
    private final MenuItem cola = new MenuItem(8, "Cola", MenuCategory.DRINKS, 45);

    @Test
    void totalAddsAllLines() {
        Cart c = new Cart();
        c.add(burger, 2);
        c.add(cola, 1);
        assertEquals(285, c.getTotal());
    }

    @Test
    void addingSameItemMergesQuantity() {
        Cart c = new Cart();
        c.add(burger, 1);
        c.add(burger, 1);
        assertEquals(1, c.getItems().size());
        assertEquals(2, c.getItems().get(0).getQuantity());
    }

    @Test
    void removeAndClearWork() {
        Cart c = new Cart();
        c.add(burger, 1);
        c.remove(1);
        assertTrue(c.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> c.remove(1));
    }

    @Test
    void badInputIsRejected() {
        Cart c = new Cart();
        assertThrows(IllegalArgumentException.class, () -> c.add(null, 1));
        assertThrows(IllegalArgumentException.class, () -> c.add(burger, 0));
    }
}

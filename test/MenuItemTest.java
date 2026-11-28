import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MenuItemTest {
    @Test
    void validItemIsCreated() {
        MenuItem m = new MenuItem(1, "  Fries ", MenuCategory.SIDES, 60);
        assertEquals("Fries", m.getName());
        assertEquals(60, m.getPrice());
        assertTrue(m.isAvailable());
    }

    @Test
    void emptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(1, " ", MenuCategory.SIDES, 60));
    }

    @Test
    void zeroOrNegativePriceIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(1, "Fries", MenuCategory.SIDES, 0));
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(1, "Fries", MenuCategory.SIDES, -5));
    }

    @Test
    void missingCategoryAndBadIdAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(1, "Fries", null, 60));
        assertThrows(IllegalArgumentException.class, () -> new MenuItem(0, "Fries", MenuCategory.SIDES, 60));
    }
}

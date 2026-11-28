import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MenuCategoryTest {
    @Test
    void hasTheFourMenuCategories() {
        assertEquals(4, MenuCategory.values().length);
    }

    @Test
    void labelsAreReadable() {
        assertEquals("Burgers", MenuCategory.BURGERS.getLabel());
        assertEquals("Chicken", MenuCategory.CHICKEN.getLabel());
        assertEquals("Sides", MenuCategory.SIDES.getLabel());
        assertEquals("Drinks", MenuCategory.DRINKS.getLabel());
    }
}

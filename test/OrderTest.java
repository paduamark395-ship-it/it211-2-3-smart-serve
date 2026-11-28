import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderTest {
    private final Customer customer = new Customer(1, "Ana", "09171234567");
    private final MenuItem burger = new MenuItem(1, "Burger", MenuCategory.BURGERS, 120);

    @Test
    void newOrderStartsAsReceived() {
        assertEquals(OrderStatus.RECEIVED, new Order(1, customer, null).getStatus());
    }

    @Test
    void sameItemIsMergedAndTotalIsCorrect() {
        Order o = new Order(1, customer, 2);
        o.addItem(burger, 1);
        o.addItem(burger, 2);
        assertEquals(1, o.getItems().size());
        assertEquals(360, o.getTotal());
    }

    @Test
    void statusMovesThroughTheFlow() {
        Order o = new Order(1, customer, null);
        o.addItem(burger, 1);
        o.advance();
        assertEquals(OrderStatus.PREPARING, o.getStatus());
        o.advance();
        o.advance();
        assertEquals(OrderStatus.COMPLETED, o.getStatus());
        assertThrows(IllegalStateException.class, o::advance);
    }

    @Test
    void emptyOrderCannotAdvance() {
        assertThrows(IllegalStateException.class, () -> new Order(1, customer, null).advance());
    }

    @Test
    void itemsCannotChangeAfterPreparingStarts() {
        Order o = new Order(1, customer, null);
        o.addItem(burger, 1);
        o.advance();
        assertThrows(IllegalStateException.class, () -> o.addItem(burger, 1));
    }

    @Test
    void badConstructorInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Order(0, customer, null));
        assertThrows(IllegalArgumentException.class, () -> new Order(1, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Order(1, customer, -1));
    }

    @Test
    void changingTheListFromOutsideCannotChangeTheOrder() {
        Order o = new Order(1, customer, null);
        o.addItem(burger, 2);
        o.advance();
        o.advance();
        o.advance();                                   // Completed
        o.getItems().get(0).setQuantity(20);           // only a copy changes
        assertEquals(240, o.getTotal());
        assertEquals(2, o.getItems().get(0).getQuantity());
    }

    @Test
    void anOrderStillReadsAfterAnItemSellsOut() {
        Order o = new Order(1, customer, null);
        o.addItem(burger, 1);
        burger.setAvailable(false);
        assertEquals(1, o.getItems().size());
    }
}

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderManagerTest {
    private final Customer customer = new Customer(1, "Ana", "09171234567");
    private final MenuItem burger = new MenuItem(1, "Burger", MenuCategory.BURGERS, 120);

    @Test
    void placingAnOrderClearsTheCart() {
        OrderManager m = new OrderManager();
        Cart cart = new Cart();
        cart.add(burger, 2);
        Order o = m.placeOrder(customer, cart, null);
        assertEquals(1, o.getId());
        assertEquals(240, o.getTotal());
        assertTrue(cart.isEmpty());
    }

    @Test
    void emptyCartIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new OrderManager().placeOrder(customer, new Cart(), null));
        assertThrows(IllegalArgumentException.class, () -> new OrderManager().placeOrder(customer, null, null));
    }

    @Test
    void ordersGetDifferentIdsAndCanAdvance() {
        OrderManager m = new OrderManager();
        Cart c1 = new Cart();
        c1.add(burger, 1);
        Cart c2 = new Cart();
        c2.add(burger, 1);
        m.placeOrder(customer, c1, null);
        Order second = m.placeOrder(customer, c2, 3);
        assertEquals(2, second.getId());
        m.advance(2);
        assertEquals(OrderStatus.PREPARING, m.find(2).getStatus());
    }

    @Test
    void unknownOrderIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new OrderManager().find(99));
    }

    @Test
    void getOrdersOfReturnsOnlyThatCustomersOrders() {
        OrderManager m = new OrderManager();
        Customer other = new Customer(2, "Ben", "09181234567");
        Cart mine = new Cart();
        mine.add(burger, 1);
        Cart theirs = new Cart();
        theirs.add(burger, 1);
        m.placeOrder(customer, mine, null);
        m.placeOrder(other, theirs, null);
        assertEquals(1, m.getOrdersOf(customer).size());
        assertEquals(1, m.getOrdersOf(other).size());
    }
}

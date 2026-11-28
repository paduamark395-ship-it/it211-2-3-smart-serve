import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Keeps every order in the system.
 * Responsibility: turn a cart into an order, find orders, and move them through the status flow.
 */
public class OrderManager {
    private final List<Order> orders = new ArrayList<>();
    private int nextId = 1;

    /** A manager with no orders yet. The first order gets number 1. */
    public OrderManager() { }

    public Order placeOrder(Customer customer, Cart cart, Integer tableNumber) {
        if (cart == null || cart.isEmpty()) throw new IllegalArgumentException("Cart is empty.");
        Order order = new Order(nextId, customer, tableNumber);
        for (OrderItem line : cart.getItems()) {
            order.addItem(line.getItem(), line.getQuantity());
        }
        orders.add(order);
        nextId++;
        cart.clear();
        return order;
    }

    public Order find(int orderId) {
        for (Order o : orders) {
            if (o.getId() == orderId) return o;
        }
        throw new IllegalArgumentException("No order with id " + orderId + ".");
    }

    public void advance(int orderId) { find(orderId).advance(); }

    /** All orders placed by one customer. A customer is recognised by phone number. */
    public List<Order> getOrdersOf(Customer customer) {
        List<Order> mine = new ArrayList<>();
        for (Order o : orders) {
            if (o.getCustomer().getPhone().equals(customer.getPhone())) mine.add(o);
        }
        return mine;
    }

    /** Used when loading saved data: adds an existing order and keeps new ids unique. */
    public void restore(Order order) {
        orders.add(order);
        nextId = Math.max(nextId, order.getId() + 1);
    }

    public List<Order> getAll() { return Collections.unmodifiableList(orders); }
}

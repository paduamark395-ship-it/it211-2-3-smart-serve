import java.util.ArrayList;
import java.util.List;

/**
 * A customer's order.
 * Responsibility: hold the items and move through the status flow in the right order.
 */
public class Order {
    private final int id;
    private final Customer customer;
    private final Integer tableNumber;          // null means pickup
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus status = OrderStatus.RECEIVED;

    public Order(int id, Customer customer, Integer tableNumber) {
        if (id <= 0) throw new IllegalArgumentException("Order id must be positive.");
        if (customer == null) throw new IllegalArgumentException("Customer is required.");
        if (tableNumber != null && tableNumber <= 0) {
            throw new IllegalArgumentException("Table number must be positive.");
        }
        this.id = id;
        this.customer = customer;
        this.tableNumber = tableNumber;
    }

    public int getId() { return id; }
    public Customer getCustomer() { return customer; }
    public Integer getTableNumber() { return tableNumber; }
    public OrderStatus getStatus() { return status; }
    /**
     * A copy of the order lines. Changing a copy does not change the order, so a quantity
     * cannot be altered after the order has started (addItem is the only way in).
     */
    public List<OrderItem> getItems() {
        List<OrderItem> copy = new ArrayList<>();
        for (OrderItem line : items) copy.add(new OrderItem(line));
        return copy;
    }

    public void addItem(MenuItem item, int quantity) {
        if (item == null) throw new IllegalArgumentException("Menu item is required.");
        if (status != OrderStatus.RECEIVED) {
            throw new IllegalStateException("Order can no longer be changed.");
        }
        for (OrderItem line : items) {
            if (line.getItem().getId() == item.getId()) {
                line.addQuantity(quantity);
                return;
            }
        }
        items.add(new OrderItem(item, quantity));
    }

    public double getTotal() {
        double total = 0;
        for (OrderItem line : items) total += line.getSubtotal();
        return total;
    }

    /** Moves the order to its next status (staff action). */
    public void advance() {
        if (items.isEmpty()) throw new IllegalStateException("Order has no items.");
        status = status.next();
    }

    @Override
    public String toString() {
        return "Order #" + id + " - " + status.getLabel() + " - P" + String.format(java.util.Locale.US, "%.2f", getTotal());
    }
}

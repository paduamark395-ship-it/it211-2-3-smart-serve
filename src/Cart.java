import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The customer's basket before they place the order.
 * Responsibility: collect items and work out the running total.
 */
public class Cart {
    private final List<OrderItem> items = new ArrayList<>();

    /** A new, empty cart. */
    public Cart() { }

    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }

    public boolean isEmpty() { return items.isEmpty(); }

    public void add(MenuItem item, int quantity) {
        if (item == null) throw new IllegalArgumentException("Menu item is required.");
        for (OrderItem line : items) {
            if (line.getItem().getId() == item.getId()) {
                line.addQuantity(quantity);
                return;
            }
        }
        items.add(new OrderItem(item, quantity));
    }

    public void remove(int menuItemId) {
        boolean removed = items.removeIf(line -> line.getItem().getId() == menuItemId);
        if (!removed) throw new IllegalArgumentException("That item is not in the cart.");
    }

    public double getTotal() {
        double total = 0;
        for (OrderItem line : items) total += line.getSubtotal();
        return total;
    }

    public void clear() { items.clear(); }
}

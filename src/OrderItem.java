/**
 * One line of an order: a menu item and how many of it.
 * Responsibility: keep the quantity valid and work out the line subtotal.
 */
public class OrderItem {
    private final MenuItem item;
    private int quantity;

    public OrderItem(MenuItem item, int quantity) {
        if (item == null) throw new IllegalArgumentException("Menu item is required.");
        if (!item.isAvailable()) throw new IllegalArgumentException(item.getName() + " is sold out.");
        this.item = item;
        setQuantity(quantity);
    }

    /** Copy of another line. It skips the sold-out check, because the order was valid when it was placed. */
    public OrderItem(OrderItem other) {
        if (other == null) throw new IllegalArgumentException("Order item is required.");
        this.item = other.item;
        this.quantity = other.quantity;
    }

    public MenuItem getItem() { return item; }
    public int getQuantity() { return quantity; }

    public void setQuantity(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be at least 1.");
        if (quantity > 20) throw new IllegalArgumentException("Quantity cannot be more than 20.");
        this.quantity = quantity;
    }

    public void addQuantity(int extra) { setQuantity(quantity + extra); }

    public double getSubtotal() { return item.getPrice() * quantity; }

    @Override
    public String toString() {
        return quantity + " x " + item.getName() + " = P" + String.format(java.util.Locale.US, "%.2f", getSubtotal());
    }
}

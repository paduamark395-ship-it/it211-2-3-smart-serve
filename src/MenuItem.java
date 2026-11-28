/**
 * One product on the menu.
 * Responsibility: hold the item's details and keep its price and name valid.
 */
public class MenuItem {
    private final int id;
    private String name;
    private final MenuCategory category;
    private double price;
    private boolean available;

    public MenuItem(int id, String name, MenuCategory category, double price) {
        if (id <= 0) throw new IllegalArgumentException("Menu item id must be positive.");
        if (category == null) throw new IllegalArgumentException("Category is required.");
        this.id = id;
        this.category = category;
        this.available = true;
        setName(name);
        setPrice(price);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public MenuCategory getCategory() { return category; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return available; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty.");
        }
        this.name = name.trim();
    }

    public void setPrice(double price) {
        if (price <= 0) throw new IllegalArgumentException("Price must be greater than zero.");
        this.price = price;
    }

    public void setAvailable(boolean available) { this.available = available; }

    @Override
    public String toString() {
        return id + ". " + name + " (" + category.getLabel() + ") - P" + String.format(java.util.Locale.US, "%.2f", price)
                + (available ? "" : " [sold out]");
    }
}

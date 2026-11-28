/**
 * The groups the digital menu is split into.
 * Responsibility: name the categories customers browse by.
 */
public enum MenuCategory {
    BURGERS("Burgers"), CHICKEN("Chicken"), SIDES("Sides"), DRINKS("Drinks");

    private final String label;

    MenuCategory(String label) { this.label = label; }

    public String getLabel() { return label; }
}

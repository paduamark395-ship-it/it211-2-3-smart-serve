import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Starts Smart Serve. It sets things up, loads saved data, and runs the console menu. */
public class Main {
    private static final Path DATA_FILE = Paths.get("smart-serve-data.txt");

    public static void main(String[] args) {
        List<MenuItem> menu = new ArrayList<>();
        menu.add(new MenuItem(1, "Classic Burger", MenuCategory.BURGERS, 120));
        menu.add(new MenuItem(2, "Cheese Burger", MenuCategory.BURGERS, 140));
        menu.add(new MenuItem(3, "Crispy Chicken", MenuCategory.CHICKEN, 150));
        menu.add(new MenuItem(4, "Chicken Tenders", MenuCategory.CHICKEN, 130));
        menu.add(new MenuItem(5, "Fries", MenuCategory.SIDES, 60));
        menu.add(new MenuItem(6, "Onion Rings", MenuCategory.SIDES, 70));
        menu.add(new MenuItem(7, "Iced Tea", MenuCategory.DRINKS, 50));
        menu.add(new MenuItem(8, "Cola", MenuCategory.DRINKS, 45));

        TableManager tables = new TableManager(Arrays.asList(
                new Table(1, 2), new Table(2, 2), new Table(3, 2),
                new Table(4, 4), new Table(5, 4), new Table(6, 4)));
        OrderManager orders = new OrderManager();

        DataStore store = new DataStore(DATA_FILE, menu);
        int skipped = 0;
        try {
            skipped = store.load(orders, tables);
        } catch (IllegalStateException e) {
            System.out.println("Warning: " + e.getMessage() + " Starting with no saved data.");
        }
        if (!orders.getAll().isEmpty() || !tables.getReservations().isEmpty()) {
            System.out.println("Loaded " + orders.getAll().size() + " order(s) and "
                    + tables.getReservations().size() + " reservation(s) from " + DATA_FILE + ".");
        }
        if (skipped > 0) {
            System.out.println("Warning: " + skipped + " saved line(s) could not be read and were skipped."
                    + " A copy of the original file was kept as " + DATA_FILE + ".bak.");
        }

        new ConsoleMenu(System.in, System.out, menu, orders, tables,
                () -> store.save(orders, tables)).run();
    }
}

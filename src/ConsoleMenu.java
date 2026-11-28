import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * The text screen of Smart Serve.
 * Responsibility: show screens, read what the user types and call the other classes.
 * It holds no business rules; the model classes do the checking.
 */
public class ConsoleMenu {
    /** How dates are shown on screen. */
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter TIME_ONLY = DateTimeFormatter.ofPattern("HH:mm");
    /** Accepted input, 24-hour clock: 2026-12-05 18:30 (leading zeros optional). STRICT refuses Feb 30. */
    private static final DateTimeFormatter INPUT_24H = DateTimeFormatter.ofPattern("uuuu-M-d H:mm")
            .withResolverStyle(ResolverStyle.STRICT);
    /** Accepted input, 12-hour clock: 2026-12-05 6:30 PM or 2026-12-05 6 PM (am/pm in any case). */
    private static final DateTimeFormatter INPUT_12H = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("uuuu-M-d h")
            .optionalStart().appendPattern(":mm").optionalEnd()
            .appendPattern(" a")
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .toFormatter(Locale.US)
            .withResolverStyle(ResolverStyle.STRICT);
    private static final String BOLD_LINE = "========================================";
    private static final String LINE = "----------------------------------------";

    private final Scanner in;
    private final PrintStream out;
    private final List<MenuItem> menu;
    private final OrderManager orders;
    private final TableManager tables;
    private final Runnable afterChange;
    private final Cart cart = new Cart();
    private Customer customer;

    /** Version that does not save anything. */
    public ConsoleMenu(InputStream input, PrintStream out, List<MenuItem> menu,
                       OrderManager orders, TableManager tables) {
        this(input, out, menu, orders, tables, () -> { });
    }

    /** afterChange runs after every order or reservation change (used to save the data). */
    public ConsoleMenu(InputStream input, PrintStream out, List<MenuItem> menu,
                       OrderManager orders, TableManager tables, Runnable afterChange) {
        this.in = new Scanner(input);
        this.out = out;
        this.menu = menu;
        this.orders = orders;
        this.tables = tables;
        this.afterChange = afterChange;
    }

    // ---------- main loop ----------

    public void run() {
        showWelcome();
        try {
            customer = askCustomer();
            out.println("Welcome, " + customer.getName() + "!");
            boolean running = true;
            while (running) {
                showOptions();
                running = handleChoice();
            }
        } catch (NoSuchElementException endOfInput) {
            out.println("Input ended.");
        }
        out.println("Thank you for using Smart Serve!");
    }

    private boolean handleChoice() {
        try {
            switch (readInt("Choose: ")) {
                case 1: showMenu(); break;
                case 2: addToCart(); break;
                case 3: showCart(); break;
                case 4: placeOrder(); break;
                case 5: trackOrders(); break;
                case 6: staffUpdateOrder(); break;
                case 7: showTables(); break;
                case 8: reserveTable(); break;
                case 9: cancelReservation(); break;
                case 10: checkOut(); break;
                case 0: return false;
                default: out.println("Please choose a number from the list.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            out.println("Error: " + e.getMessage());
        }
        return true;
    }

    // ---------- screens ----------

    private void showWelcome() {
        out.println(BOLD_LINE);
        out.println("            SMART SERVE");
        out.println("   Order, track and reserve with ease");
        out.println(BOLD_LINE);
    }

    private void showOptions() {
        out.println();
        out.println(LINE);
        out.println(" 1. View menu          5. Track my orders");
        out.println(" 2. Add to cart        6. Staff: update order");
        out.println(" 3. View cart          7. View tables");
        out.println(" 4. Place order        8. Reserve a table");
        out.println("                       9. Cancel a reservation");
        out.println(" 0. Exit              10. Check out of my table");
        out.println(LINE);
    }

    private void showMenu() {
        for (MenuCategory category : MenuCategory.values()) {
            boolean headerShown = false;
            for (MenuItem item : menu) {
                if (item.getCategory() != category) continue;
                if (!headerShown) {
                    out.println();
                    out.println(" " + category.getLabel().toUpperCase());
                    headerShown = true;
                }
                out.println(String.format("  %2d. %-18s %9s%s", item.getId(), item.getName(),
                        peso(item.getPrice()), item.isAvailable() ? "" : "  (sold out)"));
            }
        }
    }

    private void showCart() {
        if (cart.isEmpty()) {
            out.println("Your cart is empty.");
            return;
        }
        for (OrderItem line : cart.getItems()) printLine(line);
        out.println(LINE);
        out.println("  Total: " + peso(cart.getTotal()));
    }

    private void showTables() {
        LocalDateTime now = LocalDateTime.now();
        out.println();
        out.println(" TABLES");
        for (Table t : tables.getTables()) {
            String status = "Available now";
            if (tables.activeReservation(t.getNumber(), now) != null) {
                status = "Reserved now - free again at "
                        + tables.freeAgainAt(t.getNumber(), now).format(FORMAT);
            }
            out.println("  " + t + " - " + status);
            for (Reservation r : tables.getReservations()) {
                if (r.getTable().getNumber() == t.getNumber() && r.getDateTime().isAfter(now)) {
                    out.println("      Reserved: " + r.getDateTime().format(FORMAT)
                            + " until " + endLabel(r) + " (" + r.getPartySize() + " people)");
                }
            }
        }
    }

    // ---------- actions ----------

    private Customer askCustomer() {
        while (true) {
            try {
                String name = readLine("Your name: ");
                String phone = readLine("Your phone number: ");
                return new Customer(1, name, phone);
            } catch (IllegalArgumentException e) {
                out.println("Error: " + e.getMessage());
            }
        }
    }

    private void addToCart() {
        int id = readInt("Menu item number: ");
        int quantity = readInt("Quantity: ");
        MenuItem chosen = findMenuItem(id);
        cart.add(chosen, quantity);
        out.println("Added " + quantity + " x " + chosen.getName() + ". Cart total: " + peso(cart.getTotal()));
    }

    private MenuItem findMenuItem(int id) {
        for (MenuItem item : menu) {
            if (item.getId() == id) return item;
        }
        throw new IllegalArgumentException("No menu item with number " + id + ".");
    }

    private void placeOrder() {
        String text = readLine("Table number (press Enter for pickup): ");
        Integer table = null;
        if (!text.trim().isEmpty()) {
            table = parse(text);
            tables.findTable(table);     // refuses a table that does not exist
        }
        Order order = orders.placeOrder(customer, cart, table);
        out.println();
        out.println("Order placed!");
        printOrder(order);
        save();
    }

    private void trackOrders() {
        List<Order> mine = orders.getOrdersOf(customer);
        if (mine.isEmpty()) {
            out.println("You have no orders yet.");
            return;
        }
        for (Order order : mine) {
            out.println();
            printOrder(order);
        }
    }

    private void staffUpdateOrder() {
        int id = readInt("Order number: ");
        orders.advance(id);
        out.println("Status updated.");
        printOrder(orders.find(id));
        save();
    }

    private void reserveTable() {
        int number = readInt("Table number: ");
        int size = readInt("Number of people: ");
        LocalDateTime when = readDateTime();
        int minutes = readDuration();
        Reservation reservation = tables.reserve(customer, number, when, size, minutes);
        out.println("Reservation confirmed!");
        printReservation(reservation);
        save();
    }

    private void cancelReservation() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> upcoming = new ArrayList<>();
        for (Reservation r : tables.getReservationsOf(customer)) {
            if (r.getDateTime().isAfter(now)) upcoming.add(r);
        }
        if (upcoming.isEmpty()) {
            out.println("You have no upcoming reservations.");
            return;
        }
        for (Reservation r : upcoming) printReservation(r);
        int id = readInt("Reservation number to cancel: ");
        if (!containsId(upcoming, id)) {
            throw new IllegalArgumentException("That is not one of your upcoming reservations.");
        }
        tables.cancel(id);
        out.println("Reservation cancelled.");
        save();
    }

    /** The customer leaves before the time they booked; the table becomes available right away. */
    private void checkOut() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> seated = new ArrayList<>();
        for (Reservation r : tables.getReservationsOf(customer)) {
            if (r.isActiveAt(now)) seated.add(r);
        }
        if (seated.isEmpty()) {
            out.println("You are not seated at a table right now.");
            return;
        }
        for (Reservation r : seated) printReservation(r);
        int id = readInt("Reservation number to check out: ");
        if (!containsId(seated, id)) {
            throw new IllegalArgumentException("That is not one of your current reservations.");
        }
        tables.checkOut(id, now);
        out.println("Checked out. Thank you! " + tables.findReservation(id).getTable()
                + " is available again.");
        save();
    }

    private boolean containsId(List<Reservation> list, int id) {
        for (Reservation r : list) {
            if (r.getId() == id) return true;
        }
        return false;
    }

    /** Saves after a change. The change already happened, so a failed save is only a warning. */
    private void save() {
        try {
            afterChange.run();
        } catch (IllegalStateException e) {
            out.println("Warning: " + e.getMessage());
        }
    }

    // ---------- printing helpers ----------

    private void printOrder(Order order) {
        String where = order.getTableNumber() == null ? "Pickup" : "Table " + order.getTableNumber();
        out.println("  Order #" + order.getId() + " | " + where);
        for (OrderItem line : order.getItems()) printLine(line);
        out.println(LINE);
        out.println("  Total: " + peso(order.getTotal()));
        out.println("  Status: " + order.getStatus().getLabel());
        out.println("  " + progress(order));
    }

    private void printLine(OrderItem line) {
        out.println(String.format("  %-26s %9s",
                line.getQuantity() + " x " + line.getItem().getName(), peso(line.getSubtotal())));
    }

    private void printReservation(Reservation r) {
        out.println("  #" + r.getId() + "  " + r.getTable() + "  for " + r.getPartySize()
                + "  on " + r.getDateTime().format(FORMAT) + " until " + endLabel(r)
                + " (" + r.getDurationMinutes() + " min)");
    }

    /** Shows only the time when the stay ends on the same day, otherwise the full date. */
    private String endLabel(Reservation r) {
        LocalDateTime end = r.getEndTime();
        return end.toLocalDate().equals(r.getDateTime().toLocalDate())
                ? end.format(TIME_ONLY) : end.format(FORMAT);
    }

    /** Example: [X] Received  [X] Preparing  [ ] Ready  [ ] Completed */
    private String progress(Order order) {
        StringBuilder bar = new StringBuilder();
        for (OrderStatus step : OrderStatus.values()) {
            String name = step.name().charAt(0) + step.name().substring(1).toLowerCase();
            bar.append(step.ordinal() <= order.getStatus().ordinal() ? "[X] " : "[ ] ").append(name).append("  ");
        }
        return bar.toString().trim();
    }

    private String peso(double amount) {
        return String.format(Locale.US, "P%.2f", amount);
    }

    // ---------- reading input ----------

    private LocalDateTime readDateTime() {
        out.println("Examples: 2026-12-05 18:30   or   2026-12-05 6:30 PM   or   2026-12-05 6 PM");
        String text = readLine("Date and time: ").trim();
        text = text.replaceAll("(?i)\\s*(am|pm)$", " $1");      // allow "6:30PM"
        boolean twelveHour = text.toLowerCase().endsWith("am") || text.toLowerCase().endsWith("pm");
        try {
            return LocalDateTime.parse(text, twelveHour ? INPUT_12H : INPUT_24H);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Please type a real date and time, like 2026-12-05 18:30 or 2026-12-05 6:30 PM.");
        }
    }

    /** How long the customer will stay, in minutes. Enter alone means the default. */
    private int readDuration() {
        String text = readLine("How long will you stay, in minutes ("
                + Reservation.MIN_DURATION_MINUTES + " to " + Reservation.MAX_DURATION_MINUTES
                + ", press Enter for " + Reservation.DEFAULT_DURATION_MINUTES + "): ").trim();
        return text.isEmpty() ? Reservation.DEFAULT_DURATION_MINUTES : parse(text);
    }

    private String readLine(String prompt) {
        out.print(prompt);
        return in.nextLine();
    }

    private int readInt(String prompt) {
        return parse(readLine(prompt));
    }

    private int parse(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please type a whole number.");
        }
    }
}

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves orders and reservations to a text file and loads them back.
 * Responsibility: keep the data when the program closes. It holds no business rules.
 *
 * One line per record, fields separated by a tab:
 *   ORDER  id  customerId  name  phone  table(or -)  statusNumber  itemId:qty,itemId:qty
 *   RESERVATION  id  customerId  name  phone  tableNumber  dateTime  partySize  minutes  checkedOutAt(or -)
 * Older files without the last two fields still load (90 minutes, not checked out).
 */
public class DataStore {
    private final Path file;
    private final List<MenuItem> menu;

    public DataStore(Path file, List<MenuItem> menu) {
        if (file == null) throw new IllegalArgumentException("File is required.");
        if (menu == null) throw new IllegalArgumentException("Menu is required.");
        this.file = file;
        this.menu = menu;
    }

    public void save(OrderManager orders, TableManager tables) {
        List<String> lines = new ArrayList<>();
        for (Order o : orders.getAll()) lines.add(orderToLine(o));
        for (Reservation r : tables.getReservations()) lines.add(reservationToLine(r));
        // Write to a temporary file first, then swap it in, so a crash can never leave half a file.
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.write(temp, lines, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not save your data: " + e.getMessage());
        }
    }

    /**
     * Loads saved data into the managers.
     * Reservations that are already over (ended or checked out) are dropped.
     * Returns how many lines could not be read. If any were skipped, the original file is first
     * copied to a .bak file, because the next save would otherwise erase those lines for good.
     */
    public int load(OrderManager orders, TableManager tables) {
        if (!Files.exists(file)) return 0;          // first run: nothing saved yet
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read saved data: " + e.getMessage());
        }
        int skipped = 0;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            try {
                String[] p = line.split("\t", -1);
                if (p[0].equals("ORDER")) {
                    orders.restore(lineToOrder(p));
                } else if (p[0].equals("RESERVATION")) {
                    Reservation r = lineToReservation(p, tables);
                    if (r != null) tables.restore(r);
                } else {
                    skipped++;
                }
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        if (skipped > 0) backUp();
        return skipped;
    }

    private void backUp() {
        try {
            Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Could not back up the saved data: " + e.getMessage());
        }
    }

    // ---------- writing ----------

    private String orderToLine(Order o) {
        StringBuilder items = new StringBuilder();
        for (OrderItem line : o.getItems()) {
            if (items.length() > 0) items.append(",");
            items.append(line.getItem().getId()).append(":").append(line.getQuantity());
        }
        return String.join("\t", "ORDER", String.valueOf(o.getId()), customerFields(o.getCustomer()),
                o.getTableNumber() == null ? "-" : String.valueOf(o.getTableNumber()),
                String.valueOf(o.getStatus().ordinal()), items.toString());
    }

    private String reservationToLine(Reservation r) {
        return String.join("\t", "RESERVATION", String.valueOf(r.getId()), customerFields(r.getCustomer()),
                String.valueOf(r.getTable().getNumber()), r.getDateTime().toString(),
                String.valueOf(r.getPartySize()), String.valueOf(r.getDurationMinutes()),
                r.getCheckedOutAt() == null ? "-" : r.getCheckedOutAt().toString());
    }

    private String customerFields(Customer c) {
        return c.getId() + "\t" + clean(c.getName()) + "\t" + c.getPhone();
    }

    /** Tabs and line breaks would break the file format, so they become spaces. */
    private String clean(String text) {
        return text.replaceAll("[\\t\\r\\n]", " ");
    }

    // ---------- reading ----------

    private Order lineToOrder(String[] p) {
        Customer customer = new Customer(Integer.parseInt(p[2]), p[3], p[4]);
        Integer table = p[5].equals("-") ? null : Integer.valueOf(p[5]);
        Order order = new Order(Integer.parseInt(p[1]), customer, table);
        for (String part : p[7].split(",")) {
            String[] pair = part.split(":");
            order.addItem(findMenuItem(Integer.parseInt(pair[0])), Integer.parseInt(pair[1]));
        }
        int steps = Integer.parseInt(p[6]);
        for (int i = 0; i < steps; i++) order.advance();
        return order;
    }

    /** Returns null for a reservation that is already over. A stay in progress is kept. */
    private Reservation lineToReservation(String[] p, TableManager tables) {
        LocalDateTime when = LocalDateTime.parse(p[6]);
        int minutes = p.length > 8 ? Integer.parseInt(p[8]) : Reservation.DEFAULT_DURATION_MINUTES;
        LocalDateTime checkedOut = (p.length > 9 && !p[9].equals("-")) ? LocalDateTime.parse(p[9]) : null;
        Customer customer = new Customer(Integer.parseInt(p[2]), p[3], p[4]);
        Table table = tables.findTable(Integer.parseInt(p[5]));
        Reservation r = Reservation.restored(Integer.parseInt(p[1]), customer, table, when,
                Integer.parseInt(p[7]), minutes, checkedOut);
        return r.getEndTime().isBefore(LocalDateTime.now()) ? null : r;
    }

    private MenuItem findMenuItem(int id) {
        for (MenuItem item : menu) {
            if (item.getId() == id) return item;
        }
        throw new IllegalArgumentException("No menu item with id " + id + ".");
    }
}

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class DataStoreTest {
    private final Customer customer = new Customer(1, "Ana", "09171234567");
    private final MenuItem burger = new MenuItem(1, "Burger", MenuCategory.BURGERS, 120);
    private final MenuItem fries = new MenuItem(5, "Fries", MenuCategory.SIDES, 60);
    private final List<MenuItem> menu = Arrays.asList(burger, fries);
    private final LocalDateTime when = LocalDateTime.now().plusDays(1).withNano(0).withSecond(0);

    private Path tempFile() throws IOException {
        Path file = Files.createTempFile("smart-serve-test", ".txt");
        file.toFile().deleteOnExit();
        return file;
    }

    private TableManager newTables() {
        return new TableManager(Arrays.asList(new Table(1, 2), new Table(2, 4)));
    }

    @Test
    void savedOrdersAndReservationsComeBack() throws IOException {
        Path file = tempFile();
        DataStore store = new DataStore(file, menu);

        OrderManager orders = new OrderManager();
        TableManager tables = newTables();
        Cart cart = new Cart();
        cart.add(burger, 2);
        cart.add(fries, 1);
        Order order = orders.placeOrder(customer, cart, 2);
        orders.advance(order.getId());
        orders.advance(order.getId());
        tables.reserve(customer, 1, when, 2);
        store.save(orders, tables);

        OrderManager loadedOrders = new OrderManager();
        TableManager loadedTables = newTables();
        assertEquals(0, store.load(loadedOrders, loadedTables));

        Order back = loadedOrders.find(1);
        assertEquals(OrderStatus.READY, back.getStatus());
        assertEquals(300, back.getTotal());
        assertEquals(2, back.getTableNumber().intValue());
        assertEquals("Ana", back.getCustomer().getName());
        assertEquals(1, loadedTables.getReservations().size());
        assertEquals(when, loadedTables.getReservations().get(0).getDateTime());
    }

    @Test
    void newIdsContinueAfterLoadedOnes() throws IOException {
        Path file = tempFile();
        DataStore store = new DataStore(file, menu);
        OrderManager orders = new OrderManager();
        TableManager tables = newTables();
        Cart cart = new Cart();
        cart.add(burger, 1);
        orders.placeOrder(customer, cart, null);
        tables.reserve(customer, 1, when, 2);
        store.save(orders, tables);

        OrderManager loadedOrders = new OrderManager();
        TableManager loadedTables = newTables();
        store.load(loadedOrders, loadedTables);
        Cart again = new Cart();
        again.add(burger, 1);
        assertEquals(2, loadedOrders.placeOrder(customer, again, null).getId());
        assertEquals(2, loadedTables.reserve(customer, 2, when, 2).getId());
    }

    @Test
    void loadedReservationStillBlocksADoubleBooking() throws IOException {
        Path file = tempFile();
        DataStore store = new DataStore(file, menu);
        OrderManager orders = new OrderManager();
        TableManager tables = newTables();
        tables.reserve(customer, 1, when, 2);
        store.save(orders, tables);

        TableManager loadedTables = newTables();
        store.load(new OrderManager(), loadedTables);
        assertThrows(IllegalStateException.class, () -> loadedTables.reserve(customer, 1, when, 2));
    }

    @Test
    void missingFileMeansNothingToLoad() throws IOException {
        Path file = tempFile();
        Files.delete(file);
        OrderManager orders = new OrderManager();
        assertEquals(0, new DataStore(file, menu).load(orders, newTables()));
        assertTrue(orders.getAll().isEmpty());
    }

    @Test
    void unreadableLinesAreSkippedAndCounted() throws IOException {
        Path file = tempFile();
        String good = "RESERVATION\t1\t1\tAna\t09171234567\t1\t" + when + "\t2";
        Files.write(file, Arrays.asList("this is garbage", good, "ORDER\tx\ty"), StandardCharsets.UTF_8);
        TableManager tables = newTables();
        int skipped = new DataStore(file, menu).load(new OrderManager(), tables);
        assertEquals(2, skipped);
        assertEquals(1, tables.getReservations().size());
    }

    @Test
    void reservationsInThePastAreDroppedQuietly() throws IOException {
        Path file = tempFile();
        String old = "RESERVATION\t1\t1\tAna\t09171234567\t1\t" + LocalDateTime.now().minusDays(2) + "\t2";
        Files.write(file, Collections.singletonList(old), StandardCharsets.UTF_8);
        TableManager tables = newTables();
        assertEquals(0, new DataStore(file, menu).load(new OrderManager(), tables));
        assertTrue(tables.getReservations().isEmpty());
    }

    @Test
    void namesWithTabsDoNotBreakTheFile() throws IOException {
        Path file = tempFile();
        DataStore store = new DataStore(file, menu);
        TableManager tables = newTables();
        tables.reserve(new Customer(1, "Ana\tReyes", "09171234567"), 1, when, 2);
        store.save(new OrderManager(), tables);
        TableManager loaded = newTables();
        assertEquals(0, store.load(new OrderManager(), loaded));
        assertEquals("Ana Reyes", loaded.getReservations().get(0).getCustomer().getName());
    }

    @Test
    void badConstructorInputIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DataStore(null, menu));
        assertThrows(IllegalArgumentException.class, () -> new DataStore(java.nio.file.Paths.get("x"), null));
    }

    @Test
    void stayLengthAndCheckOutAreSaved() throws IOException {
        Path file = tempFile();
        DataStore store = new DataStore(file, menu);
        TableManager tables = newTables();
        tables.reserve(customer, 1, when, 2, 150);
        Reservation second = tables.reserve(customer, 2, when, 2, 60);
        tables.checkOut(second.getId(), when.plusMinutes(20));
        store.save(new OrderManager(), tables);

        TableManager loaded = newTables();
        assertEquals(0, store.load(new OrderManager(), loaded));
        assertEquals(150, loaded.findReservation(1).getDurationMinutes());
        assertFalse(loaded.findReservation(1).isCheckedOut());
        assertTrue(loaded.findReservation(2).isCheckedOut());
        assertEquals(when.plusMinutes(20), loaded.findReservation(2).getEndTime());
    }

    @Test
    void oldFilesWithoutStayLengthStillLoad() throws IOException {
        Path file = tempFile();
        String old = "RESERVATION\t1\t1\tAna\t09171234567\t1\t" + when + "\t2";
        Files.write(file, Collections.singletonList(old), StandardCharsets.UTF_8);
        TableManager tables = newTables();
        assertEquals(0, new DataStore(file, menu).load(new OrderManager(), tables));
        assertEquals(90, tables.findReservation(1).getDurationMinutes());
    }

    @Test
    void aStayInProgressSurvivesARestart() throws IOException {
        Path file = tempFile();
        LocalDateTime started = LocalDateTime.now().minusMinutes(30).withNano(0);
        String line = "RESERVATION\t1\t1\tAna\t09171234567\t1\t" + started + "\t2\t120\t-";
        Files.write(file, Collections.singletonList(line), StandardCharsets.UTF_8);
        TableManager tables = newTables();
        assertEquals(0, new DataStore(file, menu).load(new OrderManager(), tables));
        assertEquals(1, tables.getReservations().size());
        assertNotNull(tables.activeReservation(1, LocalDateTime.now()));
    }

    @Test
    void aCheckedOutStayIsDroppedOnLoad() throws IOException {
        Path file = tempFile();
        LocalDateTime started = LocalDateTime.now().minusMinutes(60).withNano(0);
        String line = "RESERVATION\t1\t1\tAna\t09171234567\t1\t" + started + "\t2\t120\t" + started.plusMinutes(20);
        Files.write(file, Collections.singletonList(line), StandardCharsets.UTF_8);
        TableManager tables = newTables();
        assertEquals(0, new DataStore(file, menu).load(new OrderManager(), tables));
        assertTrue(tables.getReservations().isEmpty());
    }

    @Test
    void savingLeavesNoTemporaryFileAndCanBeRepeated() throws IOException {
        Path file = tempFile();
        DataStore store = new DataStore(file, menu);
        TableManager tables = newTables();
        tables.reserve(customer, 1, when, 2);
        store.save(new OrderManager(), tables);
        store.save(new OrderManager(), tables);
        assertFalse(Files.exists(file.resolveSibling(file.getFileName() + ".tmp")));
        assertEquals(1, Files.readAllLines(file).size());
    }

    @Test
    void damagedLinesAreBackedUpBeforeTheyCanBeOverwritten() throws IOException {
        Path file = tempFile();
        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        backup.toFile().deleteOnExit();
        List<String> lines = Arrays.asList("this line is damaged",
                "RESERVATION\t1\t1\tAna\t09171234567\t1\t" + when + "\t2\t90\t-");
        Files.write(file, lines, StandardCharsets.UTF_8);
        DataStore store = new DataStore(file, menu);
        OrderManager orders = new OrderManager();
        TableManager tables = newTables();
        assertEquals(1, store.load(orders, tables));
        store.save(orders, tables);
        assertEquals(lines, Files.readAllLines(backup));          // the original is still safe
        assertEquals(1, Files.readAllLines(file).size());         // the new file only has good data
    }

    @Test
    void cleanFilesGetNoBackup() throws IOException {
        Path file = tempFile();
        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        Files.write(file, Collections.singletonList(
                "RESERVATION\t1\t1\tAna\t09171234567\t1\t" + when + "\t2\t90\t-"), StandardCharsets.UTF_8);
        new DataStore(file, menu).load(new OrderManager(), newTables());
        assertFalse(Files.exists(backup));
    }
}

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public class ConsoleMenuTest {
    private static final String LOGIN = "Ana\n09171234567\n";

    private String run(String input) {
        return runWith(input, new OrderManager(),
                new TableManager(Collections.singletonList(new Table(1, 2))), () -> { });
    }

    private String runWith(String input, OrderManager orders, TableManager tables, Runnable afterChange) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        MenuItem burger = new MenuItem(1, "Burger", MenuCategory.BURGERS, 120);
        new ConsoleMenu(new ByteArrayInputStream(input.getBytes()), new PrintStream(buffer),
                Arrays.asList(burger), orders, tables, afterChange).run();
        return buffer.toString();
    }

    private TableManager tablesWithStayInProgress() {
        TableManager tables = new TableManager(Collections.singletonList(new Table(1, 2)));
        tables.restore(Reservation.restored(1, new Customer(1, "Ana", "09171234567"), new Table(1, 2),
                LocalDateTime.now().minusMinutes(10), 2, 90, null));
        return tables;
    }

    // ---------- orders ----------

    @Test
    void mainFlowRunsFromStartToFinish() {
        String out = run(LOGIN + "2\n1\n2\n4\n\n6\n1\n6\n1\n6\n1\n0\n");
        assertTrue(out.contains("Order placed!"));
        assertTrue(out.contains("[X] Completed"));
    }

    @Test
    void invalidInputShowsAnErrorInsteadOfCrashing() {
        String out = run(LOGIN + "abc\n2\n1\n-3\n0\n");
        assertTrue(out.contains("Please type a whole number."));
        assertTrue(out.contains("Quantity must be at least 1."));
    }

    @Test
    void orderForAMissingTableIsRefused() {
        assertTrue(run(LOGIN + "2\n1\n1\n4\n9\n0\n").contains("No table 9."));
    }

    @Test
    void trackingShowsProgressForEveryOrder() {
        String out = run(LOGIN + "2\n1\n1\n4\n\n5\n0\n");
        assertTrue(out.contains("[X] Received  [ ] Preparing"));
    }

    @Test
    void saveHookRunsAfterEveryChange() {
        AtomicInteger saves = new AtomicInteger();
        runWith(LOGIN + "2\n1\n1\n4\n\n6\n1\n8\n1\n2\n2099-01-01 18:00\n\n9\n1\n0\n",
                new OrderManager(), new TableManager(Collections.singletonList(new Table(1, 2))),
                saves::incrementAndGet);
        assertEquals(4, saves.get());   // place order, update status, reserve, cancel
    }

    // ---------- tables and reservations ----------

    @Test
    void viewTablesDoesNotAskForADate() {
        String out = run(LOGIN + "7\n0\n");
        assertTrue(out.contains("Table 1 (2 seats) - Available now"));
        assertFalse(out.contains("Date and time"));
    }

    @Test
    void reservationCanBeMadeAndCancelled() {
        String out = run(LOGIN + "8\n1\n2\n2099-01-01 18:00\n\n9\n1\n0\n");
        assertTrue(out.contains("Reservation confirmed!"));
        assertTrue(out.contains("Reservation cancelled."));
    }

    @Test
    void cancellingWithNoReservationsIsFriendly() {
        assertTrue(run(LOGIN + "9\n0\n").contains("You have no upcoming reservations."));
    }

    @Test
    void customerChoosesHowLongToStay() {
        String out = run(LOGIN + "8\n1\n2\n2099-01-01 18:00\n120\n0\n");
        assertTrue(out.contains("on 2099-01-01 18:00 until 20:00 (120 min)"));
    }

    @Test
    void pressingEnterGivesTheDefaultStay() {
        String out = run(LOGIN + "8\n1\n2\n2099-01-01 18:00\n\n0\n");
        assertTrue(out.contains("until 19:30 (90 min)"));
    }

    @Test
    void stayLengthOutsideTheLimitsIsRefused() {
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 18:00\n10\n0\n").contains("Stay time must be between 30 and 240"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 18:00\n500\n0\n").contains("Stay time must be between 30 and 240"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 18:00\nabc\n0\n").contains("Please type a whole number."));
    }

    @Test
    void longStayBlocksALaterBookingButOtherTimesStayFree() {
        String out = run(LOGIN + "8\n1\n2\n2099-01-01 18:00\n180\n8\n1\n2\n2099-01-01 20:00\n\n8\n1\n2\n2099-01-01 21:00\n\n0\n");
        assertTrue(out.contains("already reserved at that time"));   // 20:00 is inside the 3-hour stay
        assertTrue(out.contains("on 2099-01-01 21:00"));             // 21:00 is free
    }

    @Test
    void anotherCustomerDoesNotSeeTheReservation() {
        OrderManager orders = new OrderManager();
        TableManager tables = new TableManager(Collections.singletonList(new Table(1, 2)));
        runWith(LOGIN + "8\n1\n2\n2099-01-01 18:00\n\n0\n", orders, tables, () -> { });
        String other = runWith("Ben\n09189999999\n9\n0\n", orders, tables, () -> { });
        assertTrue(other.contains("You have no upcoming reservations."));
    }

    @Test
    void sameCustomerSeesTheirReservationInANewSession() {
        OrderManager orders = new OrderManager();
        TableManager tables = new TableManager(Collections.singletonList(new Table(1, 2)));
        runWith(LOGIN + "8\n1\n2\n2099-01-01 18:00\n\n0\n", orders, tables, () -> { });
        assertTrue(runWith(LOGIN + "9\n1\n0\n", orders, tables, () -> { }).contains("Reservation cancelled."));
    }

    // ---------- early check-out ----------

    @Test
    void checkOutFreesTheTableRightAway() {
        TableManager tables = tablesWithStayInProgress();
        String out = runWith(LOGIN + "7\n10\n1\n7\n0\n", new OrderManager(), tables, () -> { });
        assertTrue(out.contains("Table 1 (2 seats) - Reserved now - free again at"));
        assertTrue(out.contains("Checked out."));
        assertTrue(out.lastIndexOf("Table 1 (2 seats) - Available now") > out.indexOf("Checked out."));
    }

    @Test
    void checkOutWhenNotSeatedIsFriendly() {
        assertTrue(run(LOGIN + "10\n0\n").contains("You are not seated at a table right now."));
    }

    @Test
    void upcomingReservationCannotBeCheckedOut() {
        String out = run(LOGIN + "8\n1\n2\n2099-01-01 18:00\n\n10\n0\n");
        assertTrue(out.contains("You are not seated at a table right now."));
    }

    @Test
    void checkOutOfSomeoneElsesReservationIsRefused() {
        TableManager tables = tablesWithStayInProgress();
        String out = runWith(LOGIN + "10\n99\n0\n", new OrderManager(), tables, () -> { });
        assertTrue(out.contains("That is not one of your current reservations."));
    }

    @Test
    void checkOutRunsTheSaveHook() {
        AtomicInteger saves = new AtomicInteger();
        runWith(LOGIN + "10\n1\n0\n", new OrderManager(), tablesWithStayInProgress(), saves::incrementAndGet);
        assertEquals(1, saves.get());
    }

    // ---------- time formats ----------

    @Test
    void reservationAcceptsTwelveHourTimeInAnyCase() {
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 6:00 PM\n\n0\n").contains("on 2099-01-01 18:00"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 6:00pm\n\n0\n").contains("on 2099-01-01 18:00"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 12:30 AM\n\n0\n").contains("on 2099-01-01 00:30"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 12:00 PM\n\n0\n").contains("on 2099-01-01 12:00"));
    }

    @Test
    void reservationAcceptsHourOnlyWithAmPm() {
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 6 PM\n\n0\n").contains("on 2099-01-01 18:00"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 6pm\n\n0\n").contains("on 2099-01-01 18:00"));
    }

    @Test
    void reservationAcceptsTwentyFourHourTimeAndMissingZeros() {
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 18:00\n\n0\n").contains("on 2099-01-01 18:00"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-1-5 6:30\n\n0\n").contains("on 2099-01-05 06:30"));
    }

    @Test
    void impossibleDatesAndTimesAreRefused() {
        assertTrue(run(LOGIN + "8\n1\n2\n2099-02-30 18:00\n0\n").contains("Please type a real date and time"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 25:00\n0\n").contains("Please type a real date and time"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 18:00 PM\n0\n").contains("Please type a real date and time"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01\n0\n").contains("Please type a real date and time"));
        assertTrue(run(LOGIN + "8\n1\n2\n2099-01-01 13 PM\n0\n").contains("Please type a real date and time"));
    }

    @Test
    void aFailedSaveIsAWarningNotAnError() {
        String out = runWith(LOGIN + "2\n1\n1\n4\n\n0\n", new OrderManager(),
                new TableManager(Collections.singletonList(new Table(1, 2))),
                () -> { throw new IllegalStateException("Could not save your data: disk full"); });
        assertTrue(out.contains("Order placed!"));
        assertTrue(out.contains("Warning: Could not save your data: disk full"));
        assertFalse(out.contains("Error: Could not save"));
    }
}

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class TableManagerTest {
    private final Customer customer = new Customer(1, "Ana", "09171234567");
    private final LocalDateTime when = LocalDateTime.now().plusDays(1);

    private TableManager manager() {
        return new TableManager(Arrays.asList(new Table(1, 2), new Table(2, 4)));
    }

    @Test
    void reservingMakesTheTableUnavailable() {
        TableManager m = manager();
        m.reserve(customer, 1, when, 2);
        assertFalse(m.isAvailable(1, when));
        assertTrue(m.isAvailable(2, when));
        assertEquals(1, m.availableTables(when).size());
    }

    @Test
    void sameTableCannotBeBookedTwice() {
        TableManager m = manager();
        m.reserve(customer, 1, when, 2);
        assertThrows(IllegalStateException.class, () -> m.reserve(customer, 1, when.plusMinutes(30), 2));
    }

    @Test
    void laterSlotIsStillFree() {
        TableManager m = manager();
        m.reserve(customer, 1, when, 2);
        assertDoesNotThrow(() -> m.reserve(customer, 1, when.plusMinutes(120), 2));
    }

    @Test
    void cancelFreesTheTable() {
        TableManager m = manager();
        Reservation r = m.reserve(customer, 1, when, 2);
        m.cancel(r.getId());
        assertTrue(m.isAvailable(1, when));
        assertThrows(IllegalArgumentException.class, () -> m.cancel(r.getId()));
    }

    @Test
    void unknownTableAndEmptyListAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> manager().reserve(customer, 9, when, 2));
        assertThrows(IllegalArgumentException.class, () -> new TableManager(null));
    }

    @Test
    void getReservationsOfReturnsOnlyThatCustomersBookings() {
        TableManager m = manager();
        Customer other = new Customer(2, "Ben", "09181234567");
        m.reserve(customer, 1, when, 2);
        m.reserve(other, 2, when, 2);
        assertEquals(1, m.getReservationsOf(customer).size());
        assertEquals(2, m.getReservationsOf(other).get(0).getTable().getNumber());
    }

    @Test
    void reservedTableIsActiveDuringTheBookingThenFreeAgain() {
        TableManager m = manager();
        Reservation r = m.reserve(customer, 1, when, 2);
        assertNull(m.activeReservation(1, when.minusMinutes(5)));
        assertEquals(r, m.activeReservation(1, when.plusMinutes(30)));
        assertNull(m.activeReservation(1, when.plusMinutes(90)));
        assertNull(m.activeReservation(2, when.plusMinutes(30)));
    }

    @Test
    void freeAgainAtGivesTheEndOfTheBooking() {
        TableManager m = manager();
        m.reserve(customer, 1, when, 2);
        assertEquals(when.plusMinutes(90), m.freeAgainAt(1, when.plusMinutes(10)));
    }

    @Test
    void freeTableIsFreeAtTheSameTime() {
        TableManager m = manager();
        assertEquals(when, m.freeAgainAt(1, when));
    }

    @Test
    void backToBackBookingsKeepTheTableBusyUntilTheLastEnds() {
        TableManager m = manager();
        m.reserve(customer, 1, when, 2);
        m.reserve(customer, 1, when.plusMinutes(90), 2);
        assertEquals(when.plusMinutes(180), m.freeAgainAt(1, when.plusMinutes(10)));
    }

    @Test
    void cancellingFreesTheTableImmediately() {
        TableManager m = manager();
        Reservation r = m.reserve(customer, 1, when, 2);
        m.cancel(r.getId());
        assertEquals(when.plusMinutes(10), m.freeAgainAt(1, when.plusMinutes(10)));
    }

    @Test
    void unknownTableIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> manager().freeAgainAt(9, when));
    }

    @Test
    void longerStayBlocksTheTableForLonger() {
        TableManager m = manager();
        m.reserve(customer, 1, when, 2, 180);
        assertFalse(m.isAvailable(1, when.plusMinutes(150), 60));
        assertTrue(m.isAvailable(1, when.plusMinutes(180), 60));
        assertEquals(when.plusMinutes(180), m.freeAgainAt(1, when.plusMinutes(10)));
    }

    @Test
    void bookingMustNotRunIntoALaterReservation() {
        TableManager m = manager();
        m.reserve(customer, 1, when.plusMinutes(120), 2, 60);
        assertThrows(IllegalStateException.class, () -> m.reserve(customer, 1, when, 2, 150));
        assertDoesNotThrow(() -> m.reserve(customer, 1, when, 2, 120));
    }

    @Test
    void checkingOutFreesTheTableAtThatMoment() {
        TableManager m = manager();
        Reservation r = m.reserve(customer, 1, when, 2, 120);
        LocalDateTime leave = when.plusMinutes(30);
        m.checkOut(r.getId(), leave);
        assertNull(m.activeReservation(1, leave));
        assertEquals(leave, m.freeAgainAt(1, leave));
        assertDoesNotThrow(() -> m.reserve(customer, 1, when.plusMinutes(40), 2, 60));
    }

    @Test
    void checkOutOfUnknownReservationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> manager().checkOut(99, when));
        assertThrows(IllegalArgumentException.class, () -> manager().findReservation(99));
    }

    @Test
    void badStayLengthIsRejectedWhenReserving() {
        assertThrows(IllegalArgumentException.class, () -> manager().reserve(customer, 1, when, 2, 10));
    }
}

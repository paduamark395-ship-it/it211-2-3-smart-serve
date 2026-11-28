import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {
    private final Customer customer = new Customer(1, "Ana", "09171234567");
    private final Table table = new Table(1, 4);
    private final LocalDateTime later = LocalDateTime.now().plusDays(1);

    @Test
    void validReservationIsCreated() {
        Reservation r = new Reservation(1, customer, table, later, 3);
        assertEquals(3, r.getPartySize());
    }

    @Test
    void pastTimeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, customer, table, LocalDateTime.now().minusDays(1), 2));
    }

    @Test
    void partyLargerThanTableIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Reservation(1, customer, table, later, 5));
        assertThrows(IllegalArgumentException.class, () -> new Reservation(1, customer, table, later, 0));
    }

    @Test
    void missingValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Reservation(1, null, table, later, 2));
        assertThrows(IllegalArgumentException.class, () -> new Reservation(1, customer, null, later, 2));
        assertThrows(IllegalArgumentException.class, () -> new Reservation(1, customer, table, null, 2));
    }

    @Test
    void clashDetectionUsesTheBookingWindow() {
        Reservation r = new Reservation(1, customer, table, later, 2);
        assertTrue(r.clashesWith(later.plusMinutes(30)));
        assertFalse(r.clashesWith(later.plusMinutes(120)));
    }

    @Test
    void endTimeIsNinetyMinutesAfterStart() {
        Reservation r = new Reservation(1, customer, table, later, 2);
        assertEquals(later.plusMinutes(90), r.getEndTime());
    }

    @Test
    void tableIsActiveFromStartUntilEndOnly() {
        Reservation r = new Reservation(1, customer, table, later, 2);
        assertFalse(r.isActiveAt(later.minusMinutes(1)));
        assertTrue(r.isActiveAt(later));
        assertTrue(r.isActiveAt(later.plusMinutes(89)));
        assertFalse(r.isActiveAt(later.plusMinutes(90)));
    }

    @Test
    void customerChoosesTheStayLength() {
        Reservation r = new Reservation(1, customer, table, later, 2, 150);
        assertEquals(150, r.getDurationMinutes());
        assertEquals(later.plusMinutes(150), r.getEndTime());
    }

    @Test
    void stayLengthOutsideTheLimitsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Reservation(1, customer, table, later, 2, 29));
        assertThrows(IllegalArgumentException.class, () -> new Reservation(1, customer, table, later, 2, 241));
        assertDoesNotThrow(() -> new Reservation(1, customer, table, later, 2, 30));
        assertDoesNotThrow(() -> new Reservation(1, customer, table, later, 2, 240));
    }

    @Test
    void checkingOutEarlyMovesTheEndTimeToNow() {
        Reservation r = new Reservation(1, customer, table, later, 2, 120);
        LocalDateTime leave = later.plusMinutes(40);
        r.checkOut(leave);
        assertTrue(r.isCheckedOut());
        assertEquals(leave, r.getEndTime());
        assertTrue(r.isActiveAt(later.plusMinutes(39)));
        assertFalse(r.isActiveAt(leave));
    }

    @Test
    void checkOutIsRefusedBeforeStartAfterEndOrTwice() {
        Reservation r = new Reservation(1, customer, table, later, 2, 60);
        assertThrows(IllegalStateException.class, () -> r.checkOut(later.minusMinutes(5)));
        assertThrows(IllegalStateException.class, () -> r.checkOut(later.plusMinutes(60)));
        r.checkOut(later.plusMinutes(20));
        assertThrows(IllegalStateException.class, () -> r.checkOut(later.plusMinutes(30)));
        assertThrows(IllegalArgumentException.class, () -> new Reservation(2, customer, table, later, 2).checkOut(null));
    }

    @Test
    void overlapUsesTheLengthOfTheNewBooking() {
        Reservation r = new Reservation(1, customer, table, later, 2, 60);
        assertTrue(r.overlaps(later.minusMinutes(30), 60));      // new booking runs into this one
        assertFalse(r.overlaps(later.minusMinutes(60), 60));     // ends exactly when this one starts
        assertFalse(r.overlaps(later.plusMinutes(60), 60));      // starts exactly when this one ends
    }

    @Test
    void restoredReservationMayStartInThePast() {
        LocalDateTime started = LocalDateTime.now().minusMinutes(20);
        Reservation r = Reservation.restored(1, customer, table, started, 2, 90, null);
        assertTrue(r.isActiveAt(LocalDateTime.now()));
        Reservation out = Reservation.restored(2, customer, table, started, 2, 90, started.plusMinutes(10));
        assertTrue(out.isCheckedOut());
        assertFalse(out.isActiveAt(LocalDateTime.now()));
    }

    @Test
    void aTimeTypedToTheCurrentMinuteIsAccepted() {
        LocalDateTime justNow = LocalDateTime.now().minusSeconds(30);
        assertDoesNotThrow(() -> new Reservation(1, customer, table, justNow, 2));
    }

    @Test
    void aTimeMoreThanAMinuteAgoIsStillRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new Reservation(1, customer, table, LocalDateTime.now().minusMinutes(5), 2));
    }
}
